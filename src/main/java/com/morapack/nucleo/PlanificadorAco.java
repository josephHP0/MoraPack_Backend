package com.morapack.nucleo;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementa el algoritmo ACO (Ant Colony Optimization) "congestion-aware"
 * para planificación de rutas logísticas.
 *
 * Características:
 * - Construcción probabilística de rutas con feromonas
 * - Heurística multi-objetivo (tiempo, espera, SLA, congestión, hops)
 * - Validación de headroom (capacidad futura)
 * - Gestión de backlog por aeropuerto
 * - Evaporación y depósito de feromonas
 *
 * Basado en PlanificadorAco del proyecto de referencia,
 * rediseñado para integración con MoraPack.
 */
@Slf4j
public class PlanificadorAco {

    // ========== Datos de entrada ==========

    private final GrafoVuelos grafo;
    private final Map<String, T01Aeropuerto> aeropuertos;
    private final ParametrosAco params;

    // ========== Estado ACO ==========

    /** Feromonas por ID de instancia de vuelo */
    private final Map<String, Double> tau;

    /** Backlog (ocupación prevista) por aeropuerto y bucket horario */
    private final Map<String, int[]> backlogByAp;

    /** Capacidad de salida por aeropuerto y bucket horario */
    private final Map<String, int[]> outCapByAp;

    /** Capacidad de entrada por aeropuerto y bucket horario */
    private final Map<String, int[]> inCapByAp;

    /** Aeropuertos con capacidad infinita (hubs principales) */
    private final Set<String> infiniteSources;

    /** Ventana temporal de la simulación */
    private Instant tStart;
    private Instant tEnd;

    /** Tamaño del bucket horario en segundos */
    private static final long BUCKET_SIZE_SECONDS = 3600; // 1 hora

    /** Random para construcción probabilística */
    private final Random random;

    /**
     * Constructor.
     *
     * @param grafo Grafo de vuelos ya expandido
     * @param aeropuertos Mapa de aeropuertos por código ICAO
     * @param params Parámetros del algoritmo ACO
     */
    public PlanificadorAco(GrafoVuelos grafo, Map<String, T01Aeropuerto> aeropuertos, ParametrosAco params) {
        this.grafo = grafo;
        this.aeropuertos = aeropuertos;
        this.params = params;

        this.tau = new HashMap<>();
        this.backlogByAp = new HashMap<>();
        this.outCapByAp = new HashMap<>();
        this.inCapByAp = new HashMap<>();
        this.infiniteSources = new HashSet<>();
        this.random = new Random();

        // Validar parámetros
        params.validate();
    }

    /**
     * Inicializa el planificador para una ventana temporal.
     *
     * @param tStart Inicio de la ventana
     * @param tEnd Fin de la ventana
     * @param hubsInfinitos Lista de códigos ICAO con capacidad infinita
     */
    public void inicializar(Instant tStart, Instant tEnd, List<String> hubsInfinitos) {
        this.tStart = tStart;
        this.tEnd = tEnd;

        log.info("Inicializando PlanificadorAco para ventana {} - {}", tStart, tEnd);

        // Identificar hubs con capacidad infinita
        infiniteSources.clear();
        if (hubsInfinitos != null) {
            infiniteSources.addAll(hubsInfinitos);
        }
        log.info("Hubs con capacidad infinita: {}", infiniteSources);

        // Inicializar feromonas
        inicializarFeromonas();

        // Inicializar estructuras de capacidad
        inicializarCapacidades();

        log.info("Inicialización completada. Instancias de vuelo: {}", grafo.getNumInstancias());
    }

    /**
     * Inicializa las feromonas en todos los arcos (vuelos).
     */
    private void inicializarFeromonas() {
        tau.clear();
        for (FlightInstance flight : grafo.getTodasLasInstancias()) {
            tau.put(flight.getInstanceId(), params.getPheromoneInit());
        }
        log.info("Feromonas inicializadas: {} arcos con valor {}", tau.size(), params.getPheromoneInit());
    }

    /**
     * Inicializa las estructuras de capacidad por aeropuerto y bucket horario.
     */
    private void inicializarCapacidades() {
        backlogByAp.clear();
        outCapByAp.clear();
        inCapByAp.clear();

        int numBuckets = calcularNumBuckets();

        // Para cada aeropuerto
        for (String codigoIcao : grafo.getAeropuertosConSalidas()) {
            backlogByAp.put(codigoIcao, new int[numBuckets]);
            outCapByAp.put(codigoIcao, new int[numBuckets]);
            inCapByAp.put(codigoIcao, new int[numBuckets]);

            // Calcular capacidades de entrada/salida por bucket
            calcularCapacidadesPorBucket(codigoIcao, numBuckets);
        }

        log.info("Capacidades inicializadas para {} aeropuertos, {} buckets horarios",
            backlogByAp.size(), numBuckets);
    }

    /**
     * Calcula las capacidades de entrada/salida por bucket horario para un aeropuerto.
     */
    private void calcularCapacidadesPorBucket(String codigoIcao, int numBuckets) {
        int[] outCap = outCapByAp.get(codigoIcao);
        int[] inCap = inCapByAp.get(codigoIcao);

        // Calcular capacidad de salida por bucket
        List<FlightInstance> salidas = grafo.getSalidasDesde(codigoIcao);
        for (FlightInstance flight : salidas) {
            int bucket = getBucket(flight.getDepUtc());
            if (bucket >= 0 && bucket < numBuckets) {
                outCap[bucket] += flight.getCapacidad();
            }
        }

        // Calcular capacidad de entrada por bucket
        List<FlightInstance> llegadas = grafo.getLlegadasA(codigoIcao);
        for (FlightInstance flight : llegadas) {
            int bucket = getBucket(flight.getArrUtc());
            if (bucket >= 0 && bucket < numBuckets) {
                inCap[bucket] += flight.getCapacidad();
            }
        }
    }

    /**
     * Calcula el número de buckets horarios para la ventana temporal.
     */
    private int calcularNumBuckets() {
        long seconds = tEnd.getEpochSecond() - tStart.getEpochSecond();
        return (int) Math.ceil(seconds / (double) BUCKET_SIZE_SECONDS) + 1;
    }

    /**
     * Obtiene el índice del bucket para un tiempo dado.
     */
    private int getBucket(Instant tiempo) {
        long seconds = tiempo.getEpochSecond() - tStart.getEpochSecond();
        return (int) (seconds / BUCKET_SIZE_SECONDS);
    }

    // ========== Búsqueda de Ruta ACO ==========

    /**
     * Busca la mejor ruta para un pedido usando ACO.
     *
     * @param pedido Pedido a planificar
     * @param origenesPermitidos Lista de aeropuertos origen permitidos
     * @return Mejor ruta encontrada, o null si no se encontró ninguna
     */
    public Ruta buscarRuta(T03Pedido pedido, List<String> origenesPermitidos) {
        String destino = pedido.getT01Idaeropuertodestino().getT01Codigoicao();
        int cantidad = pedido.getT03Cantidadpaquetes();
        Instant tiempoInicio = pedido.getT03Fechacreacion();

        log.debug("Buscando ruta ACO para pedido {} a {} ({} paquetes)",
            pedido.getId(), destino, cantidad);

        RouteSolution mejorSolucion = null;
        String mejorOrigen = null;

        // Probar desde cada origen permitido
        for (String origen : origenesPermitidos) {
            RouteSolution solucion = buscarRutaDesde(origen, destino, tiempoInicio, cantidad);

            if (solucion != null && solucion.isFactible()) {
                if (mejorSolucion == null || solucion.getTotalCost() < mejorSolucion.getTotalCost()) {
                    mejorSolucion = solucion;
                    mejorOrigen = origen;
                }
            }
        }

        if (mejorSolucion == null) {
            log.warn("No se encontró ruta factible para pedido {} a {}", pedido.getId(), destino);
            return null;
        }

        log.debug("Ruta encontrada desde {} a {} con costo {:.4f}",
            mejorOrigen, destino, mejorSolucion.getTotalCost());

        // Convertir a Ruta y retornar
        return mejorSolucion.toRuta();
    }

    /**
     * Busca una ruta desde un origen específico usando ACO.
     */
    private RouteSolution buscarRutaDesde(String origen, String destino, Instant tiempoInicio, int cantidad) {
        RouteSolution mejorSolucion = null;

        // Ejecutar iteraciones ACO
        for (int iter = 0; iter < params.getNumIteraciones(); iter++) {

            // Construir soluciones con hormigas
            for (int hormiga = 0; hormiga < params.getNumHormigas(); hormiga++) {
                RouteSolution solucion = construirRutaProbabilistica(origen, destino, tiempoInicio, cantidad);

                if (solucion != null && solucion.isFactible()) {
                    if (mejorSolucion == null || solucion.getTotalCost() < mejorSolucion.getTotalCost()) {
                        mejorSolucion = solucion;
                    }
                }
            }

            // Evaporar feromonas
            evaporarFeromonas();

            // Depositar feromonas en la mejor solución
            if (mejorSolucion != null) {
                depositarFeromonas(mejorSolucion, cantidad);
            }
        }

        return mejorSolucion;
    }

    /**
     * Construye una ruta de forma probabilística (una hormiga).
     */
    private RouteSolution construirRutaProbabilistica(String origen, String destino,
                                                       Instant tiempoActual, int cantidad) {
        RouteSolution solucion = RouteSolution.builder()
            .path(new ArrayList<>())
            .totalCost(0.0)
            .factible(true)
            .build();

        String aeropuertoActual = origen;
        Instant tiempoDisponible = tiempoActual;

        // Construir ruta con hasta maxHops saltos
        for (int hop = 0; hop < params.getMaxHops(); hop++) {

            // Si ya llegamos al destino, terminar
            if (aeropuertoActual.equals(destino)) {
                return solucion;
            }

            // Obtener candidatos
            List<FlightInstance> candidatos = obtenerCandidatos(aeropuertoActual, tiempoDisponible, cantidad);

            if (candidatos.isEmpty()) {
                solucion.setFactible(false);
                return solucion;
            }

            // Calcular heurísticas y ordenar candidatos
            List<CandidatoConCosto> candidatosConCosto = new ArrayList<>();
            for (FlightInstance flight : candidatos) {
                double costo = calcularHeuristica(flight, destino, tiempoActual, cantidad);
                candidatosConCosto.add(new CandidatoConCosto(flight, costo));
            }

            // Seleccionar top-K candidatos
            candidatosConCosto.sort(Comparator.comparingDouble(c -> c.costo));
            int K = Math.min(params.getCandidateK(), candidatosConCosto.size());
            List<CandidatoConCosto> topK = candidatosConCosto.subList(0, K);

            // Selección probabilística basada en feromona y heurística
            FlightInstance seleccionado = seleccionarProbabilistico(topK);

            if (seleccionado == null) {
                solucion.setFactible(false);
                return solucion;
            }

            // Validar headroom
            if (!validarHeadroom(seleccionado.getDestino(), seleccionado.getArrUtc(), cantidad)) {
                // Buscar alternativa en topK
                seleccionado = buscarAlternativaConHeadroom(topK, cantidad);
                if (seleccionado == null) {
                    solucion.setFactible(false);
                    return solucion;
                }
            }

            // Agregar vuelo a la solución
            solucion.agregarVuelo(seleccionado);
            double costoVuelo = calcularHeuristica(seleccionado, destino, tiempoActual, cantidad);
            solucion.agregarCosto(costoVuelo);

            // Actualizar estado
            aeropuertoActual = seleccionado.getDestino();
            tiempoDisponible = seleccionado.getArrUtc().plus(params.getDwellMin());
        }

        // Si no llegamos al destino en maxHops, marcar como no factible
        if (!aeropuertoActual.equals(destino)) {
            solucion.setFactible(false);
        }

        return solucion;
    }

    /**
     * Obtiene candidatos (vuelos salientes) desde un aeropuerto.
     */
    private List<FlightInstance> obtenerCandidatos(String aeropuerto, Instant tiempoMinimo, int cantidad) {
        List<FlightInstance> salidas = grafo.getSalidasDesdeDespuesDe(aeropuerto, tiempoMinimo);

        // Filtrar por capacidad
        double reserva = params.getReserveTransitRatio();
        return salidas.stream()
            .filter(f -> !f.isCancelado())
            .filter(f -> {
                int capUsable = (int) (grafo.getCapacidadRemanente(f.getInstanceId()) * (1.0 - reserva));
                return capUsable >= cantidad;
            })
            .collect(Collectors.toList());
    }

    /**
     * Calcula la heurística multi-objetivo para un vuelo candidato.
     */
    private double calcularHeuristica(FlightInstance flight, String destinoFinal,
                                       Instant tiempoInicioPedido, int cantidad) {
        double costo = 0.0;

        // 1. Componente de tiempo (ETA normalizado)
        double horasHastaLlegada = Duration.between(tiempoInicioPedido, flight.getArrUtc()).toHours();
        double timeNorm = Math.max(0, horasHastaLlegada) / params.getEtaRef().toHours();
        costo += params.getWTime() * timeNorm;

        // 2. Componente de espera (tiempo hasta próximo vuelo saliente)
        double esperaHoras = calcularEsperaEstimada(flight.getDestino(), flight.getArrUtc());
        double waitNorm = esperaHoras / params.getWaitRef().toHours();
        costo += params.getWWait() * waitNorm;

        // 3. Componente de riesgo SLA
        double riesgoSLA = calcularRiesgoSLA(flight.getDestino(), destinoFinal, flight.getArrUtc(), tiempoInicioPedido);
        costo += params.getWSla() * riesgoSLA;

        // 4. Componente de congestión
        double congestion = calcularCongestion(flight.getDestino(), flight.getArrUtc(), cantidad);
        costo += params.getWCong() * congestion;

        // 5. Penalización por hop
        costo += params.getWHops();

        return costo;
    }

    /**
     * Calcula la espera estimada en un aeropuerto.
     */
    private double calcularEsperaEstimada(String aeropuerto, Instant llegada) {
        Instant tiempoMinSalida = llegada.plus(params.getDwellMin());
        List<FlightInstance> salidas = grafo.getSalidasDesdeDespuesDe(aeropuerto, tiempoMinSalida);

        if (salidas.isEmpty()) {
            return params.getWaitRef().toHours() * 2; // Penalización si no hay salidas
        }

        FlightInstance proxima = salidas.get(0);
        return Duration.between(llegada, proxima.getDepUtc()).toHours();
    }

    /**
     * Calcula el riesgo de violar SLA.
     */
    private double calcularRiesgoSLA(String aeropuertoActual, String destinoFinal,
                                      Instant tiempoActual, Instant tiempoInicioPedido) {
        if (aeropuertoActual.equals(destinoFinal)) {
            return 0.0; // Ya llegamos al destino
        }

        // Estimar ETA mínimo desde aeropuertoActual a destinoFinal
        double etaMinimoHoras = estimarETAMinimo(aeropuertoActual, destinoFinal, tiempoActual);

        // Calcular SLA aplicable
        double slaHoras = calcularSLAAplicable(aeropuertoActual, destinoFinal);

        // Tiempo transcurrido desde inicio del pedido
        double transcurridoHoras = Duration.between(tiempoInicioPedido, tiempoActual).toHours();

        // Tiempo total estimado
        double tiempoTotalEstimado = transcurridoHoras + etaMinimoHoras;

        // Riesgo proporcional si excede SLA
        if (tiempoTotalEstimado > slaHoras) {
            double exceso = tiempoTotalEstimado - slaHoras;
            return exceso / slaHoras;
        }

        return 0.0;
    }

    /**
     * Estima el tiempo mínimo para llegar de un aeropuerto a otro.
     */
    private double estimarETAMinimo(String origen, String destino, Instant tiempoActual) {
        // Buscar vuelo directo más rápido
        List<FlightInstance> directos = grafo.getSalidasDesdeDespuesDe(origen, tiempoActual).stream()
            .filter(f -> f.getDestino().equals(destino))
            .limit(5)
            .collect(Collectors.toList());

        if (!directos.isEmpty()) {
            FlightInstance masRapido = directos.stream()
                .min(Comparator.comparing(f -> Duration.between(tiempoActual, f.getArrUtc())))
                .orElse(null);

            if (masRapido != null) {
                return Duration.between(tiempoActual, masRapido.getArrUtc()).toHours();
            }
        }

        // Si no hay directo, estimar con tiempo promedio (heurística simple)
        return 24.0; // Default: 1 día
    }

    /**
     * Calcula el SLA aplicable entre dos aeropuertos.
     */
    private double calcularSLAAplicable(String origen, String destino) {
        T01Aeropuerto apOrigen = aeropuertos.get(origen);
        T01Aeropuerto apDestino = aeropuertos.get(destino);

        if (apOrigen == null || apDestino == null) {
            return params.getSlaInterContinental(); // Default: inter-continental
        }

        String continenteOrigen = apOrigen.getT08Idciudad() != null
            ? apOrigen.getT08Idciudad().getT08Continente()
            : null;
        String continenteDestino = apDestino.getT08Idciudad() != null
            ? apDestino.getT08Idciudad().getT08Continente()
            : null;

        if (continenteOrigen != null && continenteOrigen.equals(continenteDestino)) {
            return params.getSlaIntraContinental(); // Mismo continente: 48h
        } else {
            return params.getSlaInterContinental(); // Diferentes continentes: 72h
        }
    }

    /**
     * Calcula la congestión en un aeropuerto.
     */
    private double calcularCongestion(String aeropuerto, Instant tiempo, int cantidad) {
        // Aeropuertos con capacidad infinita no tienen congestión
        if (infiniteSources.contains(aeropuerto)) {
            return 0.0;
        }

        T01Aeropuerto ap = aeropuertos.get(aeropuerto);
        if (ap == null || ap.getT01Capacidad() == null) {
            return 0.0;
        }

        int capacidad = ap.getT01Capacidad();
        int bucket = getBucket(tiempo);

        int[] backlog = backlogByAp.get(aeropuerto);
        if (backlog == null || bucket < 0 || bucket >= backlog.length) {
            return 0.0;
        }

        // Ocupación relativa
        double ocupacion = (backlog[bucket] + cantidad) / (double) capacidad;

        // Penalización usando función del parámetro
        return params.congestionPenalty(ocupacion);
    }

    /**
     * Valida que hay suficiente capacidad de salida futura (headroom).
     */
    private boolean validarHeadroom(String aeropuerto, Instant tiempo, int cantidad) {
        // Aeropuertos con capacidad infinita siempre pasan
        if (infiniteSources.contains(aeropuerto)) {
            return true;
        }

        T01Aeropuerto ap = aeropuertos.get(aeropuerto);
        if (ap == null || ap.getT01Capacidad() == null) {
            return true;
        }

        int capacidad = ap.getT01Capacidad();
        int bucket = getBucket(tiempo);

        int[] backlog = backlogByAp.get(aeropuerto);
        int[] outCap = outCapByAp.get(aeropuerto);

        if (backlog == null || outCap == null || bucket < 0 || bucket >= backlog.length) {
            return true;
        }

        // Validar capacidad instantánea
        if (backlog[bucket] + cantidad > capacidad) {
            return false;
        }

        // Validar capacidad de salida en horizonte de headroom
        Duration horizonte = params.getHeadroomHorizon();
        int numBucketsHorizonte = (int) (horizonte.toHours() + 1);

        int stockPico = backlog[bucket] + cantidad;
        int salidaAcumulada = 0;

        for (int i = 0; i < numBucketsHorizonte && (bucket + i) < outCap.length; i++) {
            salidaAcumulada += outCap[bucket + i];
        }

        return salidaAcumulada >= stockPico;
    }

    /**
     * Busca una alternativa en la lista de candidatos que cumpla headroom.
     */
    private FlightInstance buscarAlternativaConHeadroom(List<CandidatoConCosto> candidatos, int cantidad) {
        for (CandidatoConCosto candidato : candidatos) {
            if (validarHeadroom(candidato.flight.getDestino(), candidato.flight.getArrUtc(), cantidad)) {
                return candidato.flight;
            }
        }
        return null;
    }

    /**
     * Selecciona un vuelo de forma probabilística basada en feromona y heurística.
     */
    private FlightInstance seleccionarProbabilistico(List<CandidatoConCosto> candidatos) {
        if (candidatos.isEmpty()) {
            return null;
        }

        // Calcular pesos: w = tau^alpha * (1/costo)^beta
        double[] pesos = new double[candidatos.size()];
        double sumaPesos = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            CandidatoConCosto candidato = candidatos.get(i);
            double feromona = tau.getOrDefault(candidato.flight.getInstanceId(), params.getPheromoneInit());
            double eta = 1.0 / (1.0 + candidato.costo); // Heurística inversa

            double peso = Math.pow(feromona, params.getAlpha()) * Math.pow(eta, params.getBeta());
            pesos[i] = peso;
            sumaPesos += peso;
        }

        // Selección por ruleta
        double aleatorio = random.nextDouble() * sumaPesos;
        double acumulado = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            acumulado += pesos[i];
            if (aleatorio <= acumulado) {
                return candidatos.get(i).flight;
            }
        }

        // Fallback: retornar el primero
        return candidatos.get(0).flight;
    }

    /**
     * Evap ora las feromonas en todos los arcos.
     */
    private void evaporarFeromonas() {
        double factor = 1.0 - params.getRho();
        for (Map.Entry<String, Double> entry : tau.entrySet()) {
            double nuevoValor = Math.max(params.getPheromoneFloor(), entry.getValue() * factor);
            tau.put(entry.getKey(), nuevoValor);
        }
    }

    /**
     * Deposita feromonas en los arcos de una solución.
     */
    private void depositarFeromonas(RouteSolution solucion, int cantidad) {
        double delta = cantidad / Math.max(1.0, solucion.getTotalCost() + 1e-6);

        for (FlightInstance flight : solucion.getPath()) {
            String id = flight.getInstanceId();
            double valorActual = tau.getOrDefault(id, params.getPheromoneInit());
            tau.put(id, valorActual + delta);
        }
    }

    /**
     * Aplica los compromisos de una ruta asignada (actualiza capacidades y backlog).
     */
    public void aplicarCompromisos(Ruta ruta, int cantidad) {
        if (ruta == null || ruta.getTramos().isEmpty()) {
            return;
        }

        for (Tramo tramo : ruta.getTramos()) {
            // Actualizar capacidad del vuelo
            grafo.cargarVuelo(tramo.getInstanceId(), cantidad);

            // Actualizar backlog en destino
            String destino = tramo.getDestino();
            if (!infiniteSources.contains(destino)) {
                int bucket = getBucket(tramo.getArrUtc());
                int[] backlog = backlogByAp.get(destino);
                if (backlog != null && bucket >= 0 && bucket < backlog.length) {
                    backlog[bucket] += cantidad;
                }
            }
        }
    }

    // ========== Clase auxiliar ==========

    /**
     * Candidato con su costo heurístico precalculado.
     */
    private static class CandidatoConCosto {
        FlightInstance flight;
        double costo;

        CandidatoConCosto(FlightInstance flight, double costo) {
            this.flight = flight;
            this.costo = costo;
        }
    }
}
