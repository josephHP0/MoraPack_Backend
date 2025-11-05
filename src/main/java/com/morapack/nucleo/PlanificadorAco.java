package com.morapack.nucleo;

import com.morapack.model.*;
import com.morapack.dto.PlanificacionRespuestaDTO;
import com.morapack.dto.SplitRutaDTO;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class PlanificadorAco {
    // Constantes
    private static final double EVAPORATION_RATE = 0.1;
    private static final Duration DEFAULT_DWELL_MIN = Duration.ofMinutes(60); // Tiempo mínimo de espera: 1 hora
    private static final double DEFAULT_PHEROMONE_INIT = 0.1;

    // Componentes principales
    private final GrafoVuelos grafo;
    private final Map<String, T01Aeropuerto> aeropuertos;
    private final ParametrosAco P;
    private final Random random;

    // Ventana vigente
    private Instant wStart;
    private Instant wEnd;
    private int hours; // buckets

    // Índices de vuelos para la ventana
    private Map<String, T06VueloProgramado> vuelosPorId;
    private Map<Integer, List<T06VueloProgramado>> vuelosSalidaPorAeropuerto;
    private Map<Integer, List<T06VueloProgramado>> vuelosLlegadaPorAeropuerto;

    // Capacidad remanente por vuelo
    private Map<String, Integer> capacidadRemanente;

    // Ocupación por aeropuerto y bucket horario
    private Map<Integer, int[]> ocupacionPorAeropuerto;
    private Map<Integer, int[]> capacidadSalidaPorAeropuerto;
    private Map<Integer, int[]> capacidadLlegadaPorAeropuerto;

    // Feromonas por arco (vuelo)
    private final Map<String, Double> feromonas = new HashMap<>();

    // Almacenes infinitos (Lima, Bruselas, Baku)
    private List<Integer> almacenesInfinitos;
    
    private static Instant max(Instant a, Instant b) {
        return a != null && b != null ? (a.isAfter(b) ? a : b) : (a != null ? a : b);
    }

    private int bucket(Instant t) {
        // Ensure we don't return negative values
        long hours = Duration.between(wStart, t).toHours();
        return (int) Math.max(0, Math.min(this.hours - 1, hours));
    }

    public PlanificadorAco(GrafoVuelos grafo, Map<String, T01Aeropuerto> aeropuertos) {
        this(grafo, aeropuertos, ParametrosAco.defaults());
    }

    public PlanificadorAco(GrafoVuelos grafo, Map<String, T01Aeropuerto> aeropuertos, ParametrosAco parametros) {
        this.grafo = grafo;
        this.aeropuertos = aeropuertos;
        this.P = parametros == null ? ParametrosAco.defaults() : parametros;
        this.random = new Random(42); // Semilla fija para reproducibilidad
    }

    public List<PlanificacionRespuestaDTO> planificarSemanal(List<T03Pedido> pedidos, Instant tStart, Instant tEnd) {
        if (tStart == null || tEnd == null || tEnd.isBefore(tStart)) {
            throw new IllegalArgumentException("Invalid time window: start and end times must be valid and end must be after start");
        }

        if (Duration.between(tStart, tEnd).toDays() > 30) {
            throw new IllegalArgumentException("Time window too large: maximum 30 days");
        }

        inicializarVentana(tStart, tEnd);
        indexarVuelos(tStart, tEnd);
        prepararPronosticoCapacidades();
        inicializarFeromonas();

        // Ordenar pedidos por fecha y cantidad
        pedidos.sort(Comparator
            .comparing(T03Pedido::getT03Fechacreacion)
            .thenComparing((T03Pedido p) -> -p.getT03Cantidadpaquetes()));

        List<PlanificacionRespuestaDTO> respuesta = new ArrayList<>();
        Random rnd = new Random(42);

        for (T03Pedido pedido : pedidos) {
            T01Aeropuerto origen = pedido.getT01Idaeropuertoorigen();
            T01Aeropuerto destino = pedido.getT01Idaeropuertodestino();
            int cantidad = pedido.getT03Cantidadpaquetes();
            Instant t0 = max(pedido.getT03Fechacreacion(), wStart);

            // Diagnóstico detallado
            System.out.println("\n=== Planificando Pedido " + pedido.getId() + " ===");
            System.out.println("  Origen: " + (origen != null ? origen.getT01Codigoicao() + " (ID:" + origen.getId() + ")" : "NULL"));
            System.out.println("  Destino: " + (destino != null ? destino.getT01Codigoicao() + " (ID:" + destino.getId() + ")" : "NULL"));
            System.out.println("  Cantidad: " + cantidad + " paquetes");
            System.out.println("  Fecha pedido: " + pedido.getT03Fechacreacion());
            System.out.println("  t0 (inicio búsqueda): " + t0);
            System.out.println("  Almacenes disponibles: " + almacenesInfinitos);

            // Validar que destino no sea null
            if (destino == null) {
                System.err.println("  [ERROR] Destino NULL - PEDIDO NO FACTIBLE");
                respuesta.add(crearRespuestaVacia(pedido));
                continue;
            }

            // Validar que origen y destino sean diferentes
            if (origen != null && origen.getId().equals(destino.getId())) {
                System.err.println("  [ERROR] Origen = Destino (" + destino.getT01Codigoicao() + ") - PEDIDO NO FACTIBLE (no tiene sentido logístico)");
                respuesta.add(crearRespuestaVacia(pedido));
                continue;
            }

            // Intentaremos asignar el pedido en trozos (chunks) si no hay un vuelo único
            int remaining = cantidad;
            List<SplitRutaDTO> splitsAsignados = new ArrayList<>();
            double costeTotalPedido = 0.0;
            boolean failed = false;

            // Helper para obtener máxima capacidad utilizable en cualquier vuelo
            java.util.function.Supplier<Integer> computeMaxUtilizable = () -> {
                int max = 0;
                for (T06VueloProgramado v : vuelosPorId.values()) {
                    String vid = buildInstanceId(v);
                    int cap = capacidadRemanente.getOrDefault(vid, 0);
                    int reservado = (int) (v.getT06Capacidadtotal() * P.reserveTransitRatio);
                    int util = Math.max(0, cap - reservado);
                    if (util > max) max = util;
                }
                return max;
            };

            int intentosFallidos = 0;
            while (remaining > 0) {
                int maxUtil = computeMaxUtilizable.get();
                if (maxUtil <= 0) {
                    // Si no hay capacidad, intentar con reserva reducida
                    if (intentosFallidos < 3) {
                        intentosFallidos++;
                        System.out.println("[ACO] Sin capacidad disponible, reduciendo reserva de tránsito temporalmente (intento " + intentosFallidos + ")");
                        // Continuar para intentar con chunks más pequeños
                    } else {
                        failed = true;
                        break;
                    }
                }

                int chunk = Math.min(remaining, Math.max(1, maxUtil));
                RutaSolucion bestChunk = null;

                // Try to find a route for this chunk from any origin - múltiples intentos
                for (int intento = 0; intento < 5 && bestChunk == null; intento++) {
                    for (Integer idOrigen : almacenesInfinitos) {
                        RutaSolucion sol = buscarRutaACO(idOrigen, destino.getId(), t0, chunk, rnd);
                        if (sol != null && (bestChunk == null || sol.costoTotal < bestChunk.costoTotal)) {
                            bestChunk = sol;
                            break; // Encontramos una ruta, salir del loop
                        }
                    }
                }

                // If we didn't find for this chunk, try smaller chunks (binary/halving) until 1
                int trial = chunk;
                while (bestChunk == null && trial > 0) {
                    trial = Math.max(1, trial / 2);
                    for (Integer idOrigen : almacenesInfinitos) {
                        RutaSolucion sol = buscarRutaACO(idOrigen, destino.getId(), t0, trial, rnd);
                        if (sol != null && (bestChunk == null || sol.costoTotal < bestChunk.costoTotal)) {
                            bestChunk = sol;
                        }
                    }
                    if (trial == 1) break;
                }

                // Último intento: relajar restricciones si aún no hay solución
                if (bestChunk == null && intentosFallidos < 5) {
                    intentosFallidos++;
                    System.err.println("[ACO] Advertencia: No se pudo planificar pedido " + pedido.getId() +
                        ", cantidad restante: " + remaining + ", intento: " + intentosFallidos);

                    // Intentar con cantidad mínima (1 paquete) para no bloquear completamente
                    if (remaining > 10) {
                        trial = 1;
                        for (Integer idOrigen : almacenesInfinitos) {
                            RutaSolucion sol = buscarRutaACO(idOrigen, destino.getId(), t0, trial, rnd);
                            if (sol != null) {
                                bestChunk = sol;
                                break;
                            }
                        }
                    }
                }

                if (bestChunk == null) {
                    failed = true;
                    System.err.println("[ACO] ERROR: No se pudo encontrar ruta para pedido " + pedido.getId() +
                        ", cantidad restante: " + remaining + " paquetes");
                    break;
                }

                // Determinar cuántos paquetes se asignaron realmente
                int cantidadAsignada = Math.min(remaining, chunk);
                if (trial > 0 && trial < cantidadAsignada) {
                    cantidadAsignada = trial;
                }

                // Aplicar compromisos y feromonas para este chunk
                aplicarCompromisos(bestChunk, cantidadAsignada);
                depositarFeromonas(bestChunk, cantidadAsignada);

                costeTotalPedido += bestChunk.costoTotal;

                // Crear un SplitRutaDTO para este chunk
                SplitRutaDTO split = new SplitRutaDTO();
                split.setCantidad(cantidadAsignada);
                split.setCosto(bestChunk.costoTotal);
                List<String> vuelosRuta = new ArrayList<>();
                for (T06VueloProgramado v : bestChunk.ruta) {
                    vuelosRuta.add(String.format("MP-%d#%s", v.getId(), v.getT06Fechasalida()));
                }
                split.setVuelosRuta(vuelosRuta);
                splitsAsignados.add(split);

                remaining -= cantidadAsignada;
            }

            if (!failed) {
                // Crear respuesta agregada
                PlanificacionRespuestaDTO resp = new PlanificacionRespuestaDTO();
                resp.setPedidoId(pedido.getId());
                resp.setEstado("PLANIFICADO");
                resp.setSplits(splitsAsignados);
                // Mantener vuelos para compatibilidad (lista plana de todos los vuelos)
                List<String> todosLosVuelos = new ArrayList<>();
                for (SplitRutaDTO split : splitsAsignados) {
                    todosLosVuelos.addAll(split.getVuelosRuta());
                }
                resp.setVuelos(todosLosVuelos);
                resp.setCostoTotal(costeTotalPedido);
                respuesta.add(resp);
            } else {
                respuesta.add(crearRespuestaVacia(pedido));
            }

            evaporarFeromonas();
        }

        return respuesta;
    }

    private void inicializarVentana(Instant tStart, Instant tEnd) {
        this.wStart = tStart;
        this.wEnd = tEnd;
        this.hours = (int) Math.max(1, Duration.between(tStart, tEnd).toHours());
        
        // Identificar almacenes infinitos (Lima, Bruselas, Baku)
        this.almacenesInfinitos = aeropuertos.values().stream()
            .filter(this::esAlmacenInfinito)
            .map(T01Aeropuerto::getId)
            .collect(Collectors.toList());

        // Si no se encontraron almacenes infinitos explícitos, usar aeropuertos marcados como hub
        if (this.almacenesInfinitos.isEmpty()) {
            List<Integer> hubs = aeropuertos.values().stream()
                .filter(a -> {
                    try {
                        return a.getT08Idciudad() != null && Boolean.TRUE.equals(a.getT08Idciudad().getT08Eshub());
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .map(T01Aeropuerto::getId)
                .collect(Collectors.toList());

            if (!hubs.isEmpty()) {
                this.almacenesInfinitos = hubs;
                System.out.println("PlanificadorACO: usando hubs como orígenes: " + hubs);
            }
        }

        // Si aún está vacío, usar el primer aeropuerto disponible como origen por defecto
        if (this.almacenesInfinitos.isEmpty()) {
            Optional<T01Aeropuerto> any = aeropuertos.values().stream().findFirst();
            any.ifPresent(a -> {
                this.almacenesInfinitos = Collections.singletonList(a.getId());
                System.out.println("PlanificadorACO: no se encontraron almacenes/hubs, usando aeropuerto por defecto: " + a.getT01Codigoicao());
            });
        }
    }

    private boolean esAlmacenInfinito(T01Aeropuerto aeropuerto) {
        String icao = aeropuerto.getT01Codigoicao();
        return "SPIM".equals(icao) || "EBCI".equals(icao) || "UBBB".equals(icao);
    }

    private void indexarVuelos(Instant tStart, Instant tEnd) {
        // Obtener vuelos del grafo
        List<T06VueloProgramado> vuelos = grafo.obtenerVuelosEnPeriodo(tStart, tEnd);

        // Indexar por ID
        this.vuelosPorId = new HashMap<>();
        for (T06VueloProgramado vuelo : vuelos) {
            String id = buildInstanceId(vuelo);
            vuelosPorId.put(id, vuelo);
        }

        // Indexar por aeropuerto origen/destino
        this.vuelosSalidaPorAeropuerto = new HashMap<>();
        this.vuelosLlegadaPorAeropuerto = new HashMap<>();
        
        for (T06VueloProgramado vuelo : vuelos) {
            Integer origenId = vuelo.getT01Idaeropuertoorigen().getId();
            Integer destinoId = vuelo.getT01Idaeropuertodestino().getId();
            
            vuelosSalidaPorAeropuerto
                .computeIfAbsent(origenId, k -> new ArrayList<>())
                .add(vuelo);
            
            vuelosLlegadaPorAeropuerto
                .computeIfAbsent(destinoId, k -> new ArrayList<>())
                .add(vuelo);
        }

        // Ordenar por hora
        for (List<T06VueloProgramado> lista : vuelosSalidaPorAeropuerto.values()) {
            lista.sort(Comparator.comparing(T06VueloProgramado::getT06Fechasalida));
        }
        
        for (List<T06VueloProgramado> lista : vuelosLlegadaPorAeropuerto.values()) {
            lista.sort(Comparator.comparing(T06VueloProgramado::getT06Fechallegada));
        }

        // Inicializar capacidades remanentes
        this.capacidadRemanente = new HashMap<>();
        for (T06VueloProgramado vuelo : vuelos) {
            String id = buildInstanceId(vuelo);
            capacidadRemanente.put(id, vuelo.getT06Capacidadtotal());
        }
    }

    private static class RutaSolucion {
        final List<T06VueloProgramado> ruta;
        final double costoTotal;

        RutaSolucion(List<T06VueloProgramado> ruta, double costoTotal) {
            this.ruta = ruta;
            this.costoTotal = costoTotal;
        }
    }

    // Build instanceId string in the same format used by VueloService: MP-<id padded 3>#yyyy-MM-dd'T'HH:mm:00.000Z
    private String buildInstanceId(T06VueloProgramado vuelo) {
        java.time.LocalDateTime ldt = LocalDateTime.ofInstant(vuelo.getT06Fechasalida(), ZoneOffset.UTC);
        String hhmm = ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        return String.format("MP-%03d#%s:00.000Z", vuelo.getId(), hhmm);
    }

    private List<T06VueloProgramado> obtenerCandidatos(Integer aeropuertoId, Instant desde, int cantidad) {
        List<T06VueloProgramado> candidatos = new ArrayList<>();
        if (desde.isBefore(wStart)) {
            desde = wStart;  // Adjust start time to window start if necessary
        }
        
        List<T06VueloProgramado> vuelos = vuelosSalidaPorAeropuerto.getOrDefault(aeropuertoId, Collections.emptyList());
        
        // We'll compare the actual instants: consider flights that depart at or after 'desde' and before the planning window end
        System.out.println("Buscando vuelos: aeropuerto=" + aeropuertoId +
            " desde=" + desde + " (" + LocalDateTime.ofInstant(desde, ZoneOffset.UTC) + ")" +
            " cantidad=" + cantidad);
        System.out.println("  " + vuelos.size() + " vuelos disponibles desde este aeropuerto");

        for (T06VueloProgramado vuelo : vuelos) {
            Instant salida = vuelo.getT06Fechasalida();

            // Must depart within our planning window and not before the requested time
            if (salida.isBefore(desde) || salida.isAfter(wEnd)) {
                System.out.println("  descartando vuelo: " + vuelo.getId() +
                    " salida=" + salida + " (" + LocalDateTime.ofInstant(salida, ZoneOffset.UTC) + ") - fuera de ventana/antes de 'desde'");
                continue;
            }

            String id = buildInstanceId(vuelo);
            int capacidadDisponible = capacidadRemanente.getOrDefault(id, 0);
            int reservado = (int)(vuelo.getT06Capacidadtotal() * P.reserveTransitRatio);
            int utilizable = Math.max(0, capacidadDisponible - reservado);

            if (utilizable >= cantidad) {
                candidatos.add(vuelo);
            } else {
                System.out.println("  descartando vuelo por capacidad insuficiente: " + vuelo.getId() +
                    " utilizable=" + utilizable + " requerido=" + cantidad);
            }
        }
        
        return candidatos;
    }

    private T06VueloProgramado seleccionarSiguienteVuelo(
            List<T06VueloProgramado> candidatos, 
            Integer destino, 
            Instant tiempo,
            int cantidad,
            Random rnd) {
        
        double sumaProbabilidades = 0;
        double[] probabilidades = new double[candidatos.size()];
        
        for (int i = 0; i < candidatos.size(); i++) {
            T06VueloProgramado vuelo = candidatos.get(i);
            String id = buildInstanceId(vuelo);
            
            double feromona = feromonas.getOrDefault(id, P.pheromoneInit);
            double heuristica = 1.0 / (1.0 + calcularHeuristica(vuelo, destino, tiempo, cantidad));
            
            double prob = Math.pow(feromona, P.alpha) * Math.pow(heuristica, P.beta);
            probabilidades[i] = prob;
            sumaProbabilidades += prob;
        }
        
        if (sumaProbabilidades <= 0) return null;
        
        double r = rnd.nextDouble() * sumaProbabilidades;
        double acumulado = 0;
        
        for (int i = 0; i < candidatos.size(); i++) {
            acumulado += probabilidades[i];
            if (acumulado >= r) {
                return candidatos.get(i);
            }
        }
        
        return candidatos.get(candidatos.size() - 1);
    }

    private double calcularHeuristica(T06VueloProgramado vuelo, Integer destino, Instant tiempo, int cantidad) {
        // Normalización de tiempo
        double tiempoNorm = Math.max(0, Duration.between(tiempo, vuelo.getT06Fechallegada()).toHours()) /
                (double) P.etaRef.toHours();
        
        // Espera estimada en conexión
        double esperaNorm = estimarEsperaNormalizada(vuelo.getT01Idaeropuertodestino().getId(), vuelo.getT06Fechallegada());
        
        // Riesgo SLA
        double slaHoras = vuelo.getT01Idaeropuertoorigen().getT08Idciudad().getT08Continente().equals(
                vuelo.getT01Idaeropuertodestino().getT08Idciudad().getT08Continente()) ? 48.0 : 72.0;
        
        double llegadaEstimada = estimarTiempoLlegada(vuelo.getT01Idaeropuertodestino().getId(), destino, 
                vuelo.getT06Fechallegada().plus(P.dwellMin));
        
        double riesgoSLA = llegadaEstimada > slaHoras ? (llegadaEstimada - slaHoras) / slaHoras : 0.0;
        
        // Congestión
        double ocupacion = calcularOcupacion(vuelo.getT01Idaeropuertodestino().getId(), vuelo.getT06Fechallegada(), cantidad);
        double penalizacionCongestion = P.congestionPenalty(ocupacion);
        
        return P.w_time * tiempoNorm + 
               P.w_wait * esperaNorm + 
               P.w_sla * riesgoSLA + 
               P.w_cong * penalizacionCongestion + 
               P.w_hops;
    }
    
    private double estimarEsperaNormalizada(Integer aeropuertoId, Instant llegada) {
        Instant siguiente = llegada.plus(P.dwellMin);
        List<T06VueloProgramado> salidas = vuelosSalidaPorAeropuerto.getOrDefault(aeropuertoId, Collections.emptyList());
        
        for (T06VueloProgramado vuelo : salidas) {
            if (!vuelo.getT06Fechasalida().isBefore(siguiente)) {
                long espera = Duration.between(llegada, vuelo.getT06Fechasalida()).toHours();
                return Math.min(2.0, espera / (double) P.waitRef.toHours());
            }
        }
        
        return 1.0;
    }
    
    private double calcularOcupacion(Integer aeropuertoId, Instant momento, int cantidad) {
        T01Aeropuerto ap = aeropuertos.values().stream()
            .filter(a -> a.getId().equals(aeropuertoId))
            .findFirst()
            .orElse(null);
            
        if (ap == null || esAlmacenInfinito(ap)) return 0.0;
        
        int capacidad = Math.max(1, ap.getT01Capacidad());
        int bucket = bucket(momento);
        int ocupacionActual = ocupacionPorAeropuerto.get(aeropuertoId)[bucket] + cantidad;
        
        return Math.min(1.5, ocupacionActual / (double) capacidad);
    }

    private double estimarTiempoLlegada(Integer desde, Integer hasta, Instant inicio) {
        if (desde.equals(hasta)) return 0.0;
        
        // Búsqueda limitada a 6 saltos y 48h
        Instant limite = inicio.plus(Duration.ofHours(48));
        Integer aeropuertoActual = desde;
        Instant tiempo = inicio;
        
        for (int salto = 0; salto < 6; salto++) {
            List<T06VueloProgramado> salidas = vuelosSalidaPorAeropuerto.getOrDefault(aeropuertoActual, Collections.emptyList());
            T06VueloProgramado mejor = null;
            
            for (T06VueloProgramado vuelo : salidas) {
                if (vuelo.getT06Fechasalida().isBefore(tiempo)) continue;
                if (vuelo.getT06Fechallegada().isAfter(limite)) continue;
                
                if (mejor == null || vuelo.getT06Fechallegada().isBefore(mejor.getT06Fechallegada())) {
                    mejor = vuelo;
                }
            }
            
            if (mejor == null) break;
            
            aeropuertoActual = mejor.getT01Idaeropuertodestino().getId();
            tiempo = mejor.getT06Fechallegada().plus(P.dwellMin);
            
            if (aeropuertoActual.equals(hasta)) {
                return Duration.between(inicio, mejor.getT06Fechallegada()).toHours();
            }
        }
        
        return 72.0; // Penalizar si no encontramos ruta razonable
    }

    private boolean validarHeadroom(T01Aeropuerto aeropuerto, Instant llegada, int cantidad) {
        if (esAlmacenInfinito(aeropuerto)) return true;

        int capacidadAlmacen = Math.max(1, aeropuerto.getT01Capacidad());
        int bi = bucket(llegada);
        int bFin = Math.min(hours - 1, bi + (int)P.headroomHorizon.toHours());

        int[] ocupacion = ocupacionPorAeropuerto.get(aeropuerto.getId());
        int[] capacidadSalida = capacidadSalidaPorAeropuerto.get(aeropuerto.getId());

        if (ocupacion == null || capacidadSalida == null) {
            System.out.println("  No hay datos de ocupación/capacidad para aeropuerto " + aeropuerto.getId());
            return false;
        }

        // Calcular pico de ocupación en la ventana
        int ocupacionMaxima = 0;
        for (int b = bi; b <= bFin; b++) {
            ocupacionMaxima = Math.max(ocupacionMaxima, ocupacion[b] + cantidad);
        }

        // Calcular capacidad total de salida en la ventana
        int capacidadSalidaTotal = 0;
        for (int b = bi; b <= bFin; b++) {
            capacidadSalidaTotal += capacidadSalida[b];
        }

        // Validar restricciones
        boolean almacenOK = (ocupacion[bi] + cantidad) <= capacidadAlmacen;
        boolean flujoOK = capacidadSalidaTotal >= ocupacionMaxima;

        return almacenOK && flujoOK;
    }
        private static final int MAX_ITERACIONES = 100;
    
    private void prepararPronosticoCapacidades() {
        ocupacionPorAeropuerto = new HashMap<>();
        capacidadSalidaPorAeropuerto = new HashMap<>();
        capacidadLlegadaPorAeropuerto = new HashMap<>();
        
        // Inicializar arrays para cada aeropuerto
        for (T01Aeropuerto ap : aeropuertos.values()) {
            ocupacionPorAeropuerto.put(ap.getId(), new int[hours]);
            capacidadSalidaPorAeropuerto.put(ap.getId(), new int[hours]);
            capacidadLlegadaPorAeropuerto.put(ap.getId(), new int[hours]);
        }
        
        // Procesar vuelos para calcular capacidad por bucket horario
        for (T06VueloProgramado vuelo : vuelosPorId.values()) {
            // Obtener los buckets correspondientes
            int bSalida = bucket(vuelo.getT06Fechasalida());
            int bLlegada = bucket(vuelo.getT06Fechallegada());
            
            // Registrar capacidad de salida
            if (bSalida >= 0 && bSalida < hours) {
                int idOrigen = vuelo.getT01Idaeropuertoorigen().getId();
                capacidadSalidaPorAeropuerto.get(idOrigen)[bSalida] += vuelo.getT06Capacidadtotal();
                
                // Añadir capacidad distribuida en buckets cercanos para suavizar picos
                for (int i = 1; i <= 2; i++) {
                    int bAnt = bSalida - i;
                    int bSig = bSalida + i;
                    if (bAnt >= 0) {
                        capacidadSalidaPorAeropuerto.get(idOrigen)[bAnt] += vuelo.getT06Capacidadtotal() / (i * 2);
                    }
                    if (bSig < hours) {
                        capacidadSalidaPorAeropuerto.get(idOrigen)[bSig] += vuelo.getT06Capacidadtotal() / (i * 2);
                    }
                }
            }
            
            // Registrar capacidad de llegada y ocupación
            if (bLlegada >= 0 && bLlegada < hours) {
                int idDestino = vuelo.getT01Idaeropuertodestino().getId();
                capacidadLlegadaPorAeropuerto.get(idDestino)[bLlegada] += vuelo.getT06Capacidadtotal();
                
                // También distribuimos la capacidad de llegada
                for (int i = 1; i <= 2; i++) {
                    int bAnt = bLlegada - i;
                    int bSig = bLlegada + i;
                    if (bAnt >= 0) {
                        capacidadLlegadaPorAeropuerto.get(idDestino)[bAnt] += vuelo.getT06Capacidadtotal() / (i * 2);
                    }
                    if (bSig < hours) {
                        capacidadLlegadaPorAeropuerto.get(idDestino)[bSig] += vuelo.getT06Capacidadtotal() / (i * 2);
                    }
                }
            }
        }
        
        // Nota: no recortamos por la capacidad física del almacén aquí para mantener la
        // compatibilidad con el grafo de referencia; el headroom hará las validaciones.
    }

    private PlanificacionRespuestaDTO crearRespuestaVacia(T03Pedido pedido) {
        PlanificacionRespuestaDTO resp = new PlanificacionRespuestaDTO();
        resp.setPedidoId(pedido.getId());
        resp.setEstado("NO_FACTIBLE");
        resp.setVuelos(Collections.emptyList());
        resp.setCostoTotal(null);
        return resp;
    }

    private PlanificacionRespuestaDTO crearRespuestaPedido(T03Pedido pedido, RutaSolucion ruta) {
        PlanificacionRespuestaDTO resp = new PlanificacionRespuestaDTO();
        resp.setPedidoId(pedido.getId());
        resp.setEstado("PLANIFICADO");
    resp.setVuelos(ruta.ruta.stream()
        .map(this::buildInstanceId)
        .collect(Collectors.toList()));
        resp.setCostoTotal(ruta.costoTotal);
        return resp;
    }

    private void aplicarCompromisos(RutaSolucion ruta, int cantidad) {
        for (T06VueloProgramado vuelo : ruta.ruta) {
            String id = buildInstanceId(vuelo);
            int capacidadActual = capacidadRemanente.get(id);
            capacidadRemanente.put(id, capacidadActual - cantidad);
            
            int bLlegada = bucket(vuelo.getT06Fechallegada());
            if (bLlegada >= 0 && bLlegada < hours) {
                int[] ocupacion = ocupacionPorAeropuerto.get(vuelo.getT01Idaeropuertodestino().getId());
                for (int b = bLlegada; b < hours; b++) {
                    ocupacion[b] += cantidad;
                }
            }
        }
    }

    private void depositarFeromonas(RutaSolucion ruta, int cantidad) {
        double deposit = 1.0 / ruta.costoTotal;
        for (T06VueloProgramado vuelo : ruta.ruta) {
            String id = buildInstanceId(vuelo);
            feromonas.merge(id, deposit, Double::sum);
        }
    }

    private void evaporarFeromonas() {
        for (Map.Entry<String, Double> entry : feromonas.entrySet()) {
            entry.setValue(entry.getValue() * (1.0 - EVAPORATION_RATE));
        }
    }

    private void inicializarFeromonas() {
        // Inicializar matriz de feromonas con valores pequeños
        for (Map.Entry<Integer, List<T06VueloProgramado>> entry : vuelosSalidaPorAeropuerto.entrySet()) {
            for (T06VueloProgramado vuelo : entry.getValue()) {
                String id = buildInstanceId(vuelo);
                feromonas.put(id, 0.1); // Valor inicial de feromonas
            }
        }
    }
    
    private RutaSolucion buscarRutaACO(Integer idOrigen, Integer idDestino, Instant t0, int cantidad, Random rnd) {
        List<T06VueloProgramado> rutaActual = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();
        Integer aeropuertoActual = idOrigen;
        Instant tiempoActual = t0;
        double costoTotal = 0;
        StringBuilder debug = new StringBuilder();
        debug.append(String.format("buscarRutaACO: origen=%d destino=%d qty=%d start=%s\n", idOrigen, idDestino, cantidad, t0));
        
        visitados.add(aeropuertoActual);
        
        while (!aeropuertoActual.equals(idDestino)) {
            // Obtener vuelos candidatos desde aeropuerto actual
            List<T06VueloProgramado> candidatos = obtenerCandidatos(aeropuertoActual, tiempoActual, cantidad);
            
            if (candidatos.isEmpty()) {
                debug.append("  no candidates from ").append(aeropuertoActual).append(" at ").append(tiempoActual).append(" for qty ").append(cantidad).append("\n");
                System.out.println(debug.toString());
                return null;  // No hay camino posible
            }
            
            // Seleccionar siguiente vuelo
            T06VueloProgramado siguienteVuelo = seleccionarSiguienteVuelo(candidatos, idDestino, tiempoActual, cantidad, rnd);
            
            if (siguienteVuelo == null) {
                debug.append("  no valid flight selected from candidates\n");
                System.out.println(debug.toString());
                return null;  // No hay opción válida
            }
            
            // Validar capacidad de almacén en destino
            if (!validarHeadroom(siguienteVuelo.getT01Idaeropuertodestino(), siguienteVuelo.getT06Fechallegada(), cantidad)) {
                debug.append("  headroom validation failed at airport ").append(siguienteVuelo.getT01Idaeropuertodestino().getId()).append("\n");
                System.out.println(debug.toString());
                return null;  // Destino no puede recibir la carga
            }
            
            rutaActual.add(siguienteVuelo);
            aeropuertoActual = siguienteVuelo.getT01Idaeropuertodestino().getId();
            tiempoActual = siguienteVuelo.getT06Fechallegada().plus(Duration.ofMinutes(60)); // Tiempo mínimo de conexión: 1 hora
            costoTotal += calcularHeuristica(siguienteVuelo, idDestino, tiempoActual, cantidad);
            
            if (!visitados.add(aeropuertoActual)) {
                debug.append("  cycle detected at ").append(aeropuertoActual).append("\n");
                System.out.println(debug.toString());
                return null;  // Ciclo detectado
            }
            
            if (rutaActual.size() > 5) {
                debug.append("  too many hops (>5)\n");
                System.out.println(debug.toString());
                return null;  // Demasiadas escalas
            }
        }
        
        debug.append("  route found size=").append(rutaActual.size()).append(" cost=").append(costoTotal).append("\n");
        System.out.println(debug.toString());
        return new RutaSolucion(rutaActual, costoTotal);
    }
}
