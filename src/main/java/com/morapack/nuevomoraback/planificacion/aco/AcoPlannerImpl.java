package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T01Aeropuerto;
import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.repository.AeropuertoRepository;
import com.morapack.nuevomoraback.common.repository.VueloProgramadoRepository;
import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import com.morapack.nuevomoraback.planificacion.domain.T09TramoAsignado;
import com.morapack.nuevomoraback.planificacion.service.VueloExpansionService;
import com.morapack.nuevomoraback.planificacion.service.VueloExpansionService.VueloVirtual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcoPlannerImpl implements AcoPlanner {

    private final AcoParameters params;
    private final HeuristicCalculator heuristicCalculator;
    private final PheromoneMatrix pheromoneMatrix;
    private final VueloProgramadoRepository vueloRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final VueloExpansionService vueloExpansionService;
    private final Random random = new Random();

    // Cache de vuelos expandidos para la simulación actual
    private List<VueloVirtual> vuelosExpandidosCache;

    // Índice de vuelos por aeropuerto de destino para búsqueda rápida O(1)
    private Map<Integer, List<VueloVirtual>> vuelosPorDestino;

    // IDs de aeropuertos HUB (Lima, Bruselas, Baku)
    private Set<Integer> aeropuertosHubIds;

    @Override
    public List<T08RutaPlaneada> planificar(List<T02Pedido> pedidos,
                                             T08RutaPlaneada.TipoSimulacion tipoSimulacion,
                                             Instant fechaInicio,
                                             Instant fechaFin) {

        log.info("========================================");
        log.info("INICIANDO PLANIFICACIÓN ACO");
        log.info("========================================");
        log.info("Pedidos a planificar: {}", pedidos.size());
        log.info("Tipo simulación: {}", tipoSimulacion);
        log.info("Periodo: {} a {}", fechaInicio, fechaFin);
        log.info("Parámetros ACO:");
        log.info("  - Iteraciones: {}", params.getNumeroIteraciones());
        log.info("  - Hormigas por iteración: {}", params.getNumeroHormigas());
        log.info("  - Total de construcciones: {}", params.getNumeroIteraciones() * params.getNumeroHormigas());
        log.info("========================================");

        // 0. Cargar aeropuertos HUB (Lima, Bruselas, Baku)
        if (aeropuertosHubIds == null) {
            log.info("Cargando aeropuertos HUB...");
            List<T01Aeropuerto> hubs = aeropuertoRepository.findAeropuertosHub();
            aeropuertosHubIds = hubs.stream()
                .map(T01Aeropuerto::getId)
                .collect(Collectors.toSet());
            log.info("✓ Aeropuertos HUB cargados: {} (IDs: {})",
                hubs.stream().map(T01Aeropuerto::getT01CodigoIcao).collect(Collectors.joining(", ")),
                aeropuertosHubIds);
        }

        // 1. Expandir vuelos plantilla para todo el periodo de simulación
        log.info("Cargando vuelos plantilla con JOIN FETCH (aeropuertos y aviones)...");
        long tiempoCargaInicio = System.currentTimeMillis();
        List<T04VueloProgramado> vuelosPlantilla = vueloRepository.findAllVuelosPlantilla();
        long tiempoCarga = System.currentTimeMillis() - tiempoCargaInicio;
        log.info("✓ Cargados {} vuelos plantilla desde BD en {}ms (con relaciones EAGER)",
                 vuelosPlantilla.size(), tiempoCarga);

        if (vuelosPlantilla.isEmpty()) {
            log.error("ERROR: No hay vuelos plantilla en la BD. Verificar tabla T04_VUELO_PROGRAMADO");
            return new ArrayList<>();
        }

        vuelosExpandidosCache = vueloExpansionService.expandirVuelos(vuelosPlantilla, fechaInicio, fechaFin);
        log.info("✓ Vuelos expandidos: {} vuelos virtuales generados", vuelosExpandidosCache.size());

        // 1.5. Crear índice de vuelos por aeropuerto de destino para búsqueda O(1)
        log.info("Creando índice de vuelos por aeropuerto de destino...");
        long tiempoIndexInicio = System.currentTimeMillis();
        vuelosPorDestino = new HashMap<>();
        for (VueloVirtual vuelo : vuelosExpandidosCache) {
            Integer destinoId = vuelo.getDestino().getId();
            vuelosPorDestino.computeIfAbsent(destinoId, k -> new ArrayList<>()).add(vuelo);
        }
        long tiempoIndex = System.currentTimeMillis() - tiempoIndexInicio;
        log.info("✓ Índice creado: {} aeropuertos destino indexados en {}ms", vuelosPorDestino.size(), tiempoIndex);

        if (pedidos.isEmpty()) {
            log.warn("ADVERTENCIA: No hay pedidos para planificar. Verificar query findPedidosNoHubBetween");
            return new ArrayList<>();
        }

        // 2. Inicializar matriz de feromonas
        pheromoneMatrix.inicializar();

        Ant mejorHormiga = null;
        double mejorCosto = Double.MAX_VALUE;

        // 3. Iteraciones del algoritmo ACO
        log.info("Iniciando {} iteraciones con {} hormigas cada una",
                 params.getNumeroIteraciones(), params.getNumeroHormigas());

        long tiempoInicioTotal = System.currentTimeMillis();

        for (int iter = 0; iter < params.getNumeroIteraciones(); iter++) {
            long tiempoInicioIter = System.currentTimeMillis();
            List<Ant> hormigas = new ArrayList<>();

            log.info("=== Iteración {}/{} ===", iter + 1, params.getNumeroIteraciones());

            // Construir soluciones con cada hormiga
            for (int h = 0; h < params.getNumeroHormigas(); h++) {
                long tiempoInicioHormiga = System.currentTimeMillis();

                Ant hormiga = construirSolucion(pedidos);
                hormiga.calcularCosto();
                hormigas.add(hormiga);

                if (hormiga.getCostoTotal() < mejorCosto) {
                    mejorCosto = hormiga.getCostoTotal();
                    mejorHormiga = hormiga;
                    log.info("  ✓ Nueva mejor solución encontrada en hormiga {}: costo={}, pedidos asignados={}",
                             h + 1, mejorCosto, mejorHormiga.getSolucion().size());
                }

                long tiempoHormiga = System.currentTimeMillis() - tiempoInicioHormiga;

                // Log cada 10 hormigas para no saturar
                if ((h + 1) % 10 == 0) {
                    log.info("  Hormigas construidas: {}/{} (última: {}ms, pedidos asignados: {})",
                             h + 1, params.getNumeroHormigas(), tiempoHormiga, hormiga.getSolucion().size());
                }
            }

            // Actualizar feromonas
            log.debug("  Actualizando feromonas...");
            actualizarFeromonas(hormigas);
            pheromoneMatrix.evaporar();

            long tiempoIter = System.currentTimeMillis() - tiempoInicioIter;
            log.info("  Iteración {} completada en {}ms - Mejor costo actual: {}",
                     iter + 1, tiempoIter, mejorCosto);
        }

        long tiempoTotal = System.currentTimeMillis() - tiempoInicioTotal;
        log.info("Todas las iteraciones completadas en {}ms ({} segundos)",
                 tiempoTotal, tiempoTotal / 1000.0);

        // 4. Convertir mejor solución a entidades T08RutaPlaneada
        if (mejorHormiga == null || mejorHormiga.getSolucion().isEmpty()) {
            log.warn("ADVERTENCIA: No se encontró ninguna solución válida. Ningún pedido pudo ser asignado a vuelos.");
            return new ArrayList<>();
        }

        log.info("========================================");
        log.info("✓ ACO completado");
        log.info("Mejor solución: {} pedidos planificados", mejorHormiga.getSolucion().size());
        log.info("Costo total: {}", mejorCosto);
        log.info("========================================");

        return convertirSolucion(mejorHormiga, tipoSimulacion);
    }

    private Ant construirSolucion(List<T02Pedido> pedidos) {
        Ant hormiga = new Ant();
        int asignados = 0;
        int sinCandidatos = 0;

        log.trace("Construyendo solución para {} pedidos", pedidos.size());
        long tiempoInicio = System.currentTimeMillis();

        for (int i = 0; i < pedidos.size(); i++) {
            T02Pedido pedido = pedidos.get(i);

            // Log cada 50 pedidos para monitorear progreso
            if (i > 0 && i % 50 == 0) {
                long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
                log.trace("  Procesando pedido {}/{} ({}ms transcurridos)", i, pedidos.size(), tiempoTranscurrido);
            }

            // Obtener vuelos candidatos desde la caché de vuelos expandidos
            List<VueloVirtual> vuelosCandidatos = obtenerVuelosCandidatos(pedido);

            if (vuelosCandidatos.isEmpty()) {
                sinCandidatos++;
                continue; // Pedido no puede ser asignado
            }

            // Seleccionar vuelo usando probabilidad basada en feromona y heurística
            VueloVirtual vueloSeleccionado = seleccionarVuelo(pedido, vuelosCandidatos);
            hormiga.asignarVuelo(pedido, vueloSeleccionado.toEntity());
            asignados++;
        }

        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        log.trace("Hormiga construida en {}ms: {} pedidos asignados, {} sin candidatos",
                  tiempoTotal, asignados, sinCandidatos);

        return hormiga;
    }

    private List<VueloVirtual> obtenerVuelosCandidatos(T02Pedido pedido) {
        // OPTIMIZACIÓN: Usar índice por aeropuerto de destino para reducir búsqueda de O(n) a O(1)
        // Solo buscamos entre vuelos que llegan al destino del pedido

        // IMPORTANTE: Usamos el DÍA del pedido, no la hora exacta
        // Esto permite que todos los vuelos del mismo día o posteriores sean candidatos
        LocalDate diaPedido = pedido.getT02FechaPedido().atZone(ZoneId.of("UTC")).toLocalDate();
        Instant fechaMinima = diaPedido.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant fechaMaxima = diaPedido.plusDays(7).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // Verificación: si el índice es nulo o vacío
        if (vuelosPorDestino == null || vuelosPorDestino.isEmpty()) {
            log.error("ERROR CRÍTICO: vuelosPorDestino está vacío o nulo!");
            return new ArrayList<>();
        }

        // Obtener el aeropuerto de destino del pedido
        Integer destinoId = pedido.getT02IdAeropDestino().getId();

        // Buscar SOLO en los vuelos que llegan al destino del pedido
        // Esto reduce la búsqueda de ~25,794 vuelos a ~400-500 vuelos por aeropuerto
        List<VueloVirtual> vuelosHaciaDestino = vuelosPorDestino.getOrDefault(destinoId, new ArrayList<>());

        if (vuelosHaciaDestino.isEmpty()) {
            log.debug("Pedido {}: No hay vuelos hacia el aeropuerto destino {}", pedido.getId(), destinoId);
            return new ArrayList<>();
        }

        // Filtrar vuelos viables
        List<VueloVirtual> candidatos = vuelosHaciaDestino.stream()
            .filter(v -> !v.getFechaSalida().isBefore(fechaMinima)) // >= fecha mínima (mismo día o después)
            .filter(v -> v.getFechaSalida().isBefore(fechaMaxima))  // < fecha máxima (dentro de 7 días)
            .filter(v -> v.getCapacidadDisponible() >= pedido.getT02Cantidad())
            .filter(v -> !"CANCELADO".equals(v.getEstado()))
            // RESTRICCIÓN: Solo vuelos que salen de HUBs (Lima, Bruselas, Baku)
            .filter(v -> esVueloDesdeHub(v))
            .collect(Collectors.toList());

        if (candidatos.isEmpty()) {
            log.debug("Pedido {}: No se encontraron vuelos candidatos (destino={}, dia={}, cantidad={})",
                     pedido.getId(), destinoId, diaPedido, pedido.getT02Cantidad());
        } else {
            log.trace("Pedido {}: {} vuelos candidatos encontrados (de {} hacia destino {})",
                     pedido.getId(), candidatos.size(), vuelosHaciaDestino.size(), destinoId);
        }

        return candidatos;
    }

    private VueloVirtual seleccionarVuelo(T02Pedido pedido, List<VueloVirtual> candidatos) {

        if (candidatos == null || candidatos.isEmpty()) {
            log.error("ERROR: seleccionarVuelo llamado con candidatos vacíos para pedido {}", pedido.getId());
            throw new IllegalArgumentException("La lista de candidatos no puede estar vacía");
        }

        double[] probabilidades = new double[candidatos.size()];
        double suma = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            VueloVirtual vuelo = candidatos.get(i);

            // Usar plantillaId para la feromona (múltiples vuelos virtuales del mismo vuelo base)
            double feromona = pheromoneMatrix.obtenerFeromona(pedido.getId(), vuelo.getPlantillaId());
            double heuristica = heuristicCalculator.calcularHeuristica(pedido, vuelo.toEntity(), vuelo.getFechaSalida());

            // Validar valores
            if (Double.isNaN(feromona) || Double.isInfinite(feromona)) {
                log.warn("Feromona inválida para pedido {} y vuelo {}: {}", pedido.getId(), vuelo.getPlantillaId(), feromona);
                feromona = params.getFeromonaInicial();
            }
            if (Double.isNaN(heuristica) || Double.isInfinite(heuristica)) {
                log.warn("Heurística inválida para pedido {} y vuelo {}: {}", pedido.getId(), vuelo.getPlantillaId(), heuristica);
                heuristica = 0.01;
            }

            probabilidades[i] = Math.pow(feromona, params.getAlpha()) *
                               Math.pow(heuristica, params.getBeta());
            suma += probabilidades[i];
        }

        // Normalizar y seleccionar
        if (suma == 0.0) {
            log.trace("Suma de probabilidades es 0, seleccionando vuelo aleatorio");
            return candidatos.get(random.nextInt(candidatos.size()));
        }

        double rand = random.nextDouble() * suma;
        double acumulado = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            acumulado += probabilidades[i];
            if (rand <= acumulado) {
                return candidatos.get(i);
            }
        }

        return candidatos.get(candidatos.size() - 1);
    }

    private void actualizarFeromonas(List<Ant> hormigas) {
        for (Ant hormiga : hormigas) {
            double deposito = 1.0 / (hormiga.getCostoTotal() + 1.0);

            hormiga.getSolucion().forEach((pedido, vuelos) -> {
                vuelos.forEach(vuelo -> {
                    pheromoneMatrix.actualizarFeromona(pedido.getId(), vuelo.getId(), deposito);
                });
            });
        }
    }

    private List<T08RutaPlaneada> convertirSolucion(Ant mejorHormiga,
                                                     T08RutaPlaneada.TipoSimulacion tipoSimulacion) {
        List<T08RutaPlaneada> rutas = new ArrayList<>();

        if (mejorHormiga == null) {
            return rutas;
        }

        mejorHormiga.getSolucion().forEach((pedido, vuelos) -> {
            T08RutaPlaneada ruta = new T08RutaPlaneada();
            ruta.setPedido(pedido);
            ruta.setFechaPlanificacion(Instant.now());
            ruta.setTipoSimulacion(tipoSimulacion);
            ruta.setEstado(T08RutaPlaneada.EstadoRuta.PENDIENTE);

            List<T09TramoAsignado> tramos = new ArrayList<>();
            for (int i = 0; i < vuelos.size(); i++) {
                T09TramoAsignado tramo = new T09TramoAsignado();
                tramo.setRutaPlaneada(ruta);
                tramo.setVueloProgramado(vuelos.get(i));
                tramo.setCantidadProductos(pedido.getT02Cantidad());
                tramo.setOrdenEnRuta((short) (i + 1));
                tramo.setEsVueloFinal(i == vuelos.size() - 1);
                tramos.add(tramo);
            }

            ruta.setTramosAsignados(tramos);
            rutas.add(ruta);
        });

        return rutas;
    }

    /**
     * Verifica si un vuelo sale de un aeropuerto HUB (Lima, Bruselas, Baku)
     */
    private boolean esVueloDesdeHub(VueloVirtual vuelo) {
        Integer origenId = vuelo.getOrigen().getId();
        boolean esHub = aeropuertosHubIds != null && aeropuertosHubIds.contains(origenId);

        if (!esHub) {
            log.trace("Vuelo {} descartado: origen {} no es HUB",
                vuelo.getPlantillaId(),
                vuelo.getOrigen().getT01CodigoIcao());
        }

        return esHub;
    }
}
