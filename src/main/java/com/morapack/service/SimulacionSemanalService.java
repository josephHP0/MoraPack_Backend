package com.morapack.service;

import com.morapack.dto.*;
import com.morapack.model.*;
import com.morapack.nucleo.*;
import com.morapack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de simulaciones semanales de planificación logística.
 *
 * Responsabilidades:
 * - Orquestar la simulación semanal completa
 * - Cargar datos de BD (vuelos, aeropuertos, pedidos)
 * - Inicializar y ejecutar PlanificadorAco
 * - Persistir resultados (asignaciones, métricas, alertas)
 * - Generar estadísticas y reportes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacionSemanalService {

    // Repositorios
    private final T10SimulacionSemanalRepository simulacionRepository;
    private final T03PedidoRepository pedidoRepository;
    private final T06VueloProgramadoRepository vueloRepository;
    private final T01AeropuertoRepository aeropuertoRepository;
    private final T09AsignacionRepository asignacionRepository;
    private final T13MetricaDiariaRepository metricaDiariaRepository;
    private final T14AlertaNearCollapseRepository alertaRepository;

    // Hubs por defecto con capacidad infinita
    private static final List<String> HUBS_DEFAULT = List.of("SPIM", "EBCI", "UBBB");

    /**
     * Inicia una nueva simulación semanal.
     */
    @Transactional
    public SimulacionResponseDto iniciarSimulacion(SimulacionRequestDto request) {
        log.info("Iniciando simulación semanal: {} - {}", request.getFechaInicio(), request.getFechaFin());

        // Validar request
        validarRequest(request);

        // Crear registro de simulación
        T10SimulacionSemanal simulacion = T10SimulacionSemanal.builder()
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .fechaCreacion(Instant.now())
            .estado("EN_PROGRESO")
            .pedidosProcesados(0)
            .pedidosAsignados(0)
            .pedidosPendientes(0)
            .build();

        simulacion = simulacionRepository.save(simulacion);
        log.info("Simulación creada con ID: {}", simulacion.getId());

        try {
            // Ejecutar simulación
            ejecutarSimulacion(simulacion, request);

            // Actualizar estado
            simulacion.setEstado("COMPLETADA");
            simulacionRepository.save(simulacion);

            log.info("Simulación {} completada exitosamente", simulacion.getId());

            return mapToResponseDto(simulacion, "Simulación iniciada y completada exitosamente");

        } catch (Exception e) {
            log.error("Error en simulación {}: {}", simulacion.getId(), e.getMessage(), e);

            simulacion.setEstado("FALLIDA");
            simulacion.setMotivoFallo(e.getMessage());
            simulacionRepository.save(simulacion);

            return mapToResponseDto(simulacion, "Error: " + e.getMessage());
        }
    }

    /**
     * Ejecuta la simulación completa.
     */
    private void ejecutarSimulacion(T10SimulacionSemanal simulacion, SimulacionRequestDto request) {
        long startTime = System.currentTimeMillis();

        // 1. Cargar datos
        log.info("Cargando datos de BD...");
        List<T06VueloProgramado> vuelos = cargarVuelos(request.getFechaInicio(), request.getFechaFin());
        List<T01Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        List<T03Pedido> pedidos = cargarPedidos(request);

        log.info("Datos cargados: {} vuelos, {} aeropuertos, {} pedidos",
            vuelos.size(), aeropuertos.size(), pedidos.size());

        // 2. Crear GrafoVuelos y expandir instancias
        log.info("Expandiendo instancias de vuelo...");
        GrafoVuelos grafo = new GrafoVuelos(vuelos, aeropuertos);
        grafo.expandirInstancias(request.getFechaInicio(), request.getFechaFin());

        // 3. Crear mapa de aeropuertos
        Map<String, T01Aeropuerto> aeropuertoMap = aeropuertos.stream()
            .collect(Collectors.toMap(T01Aeropuerto::getT01Codigoicao, ap -> ap));

        // 4. Inicializar PlanificadorAco
        log.info("Inicializando PlanificadorAco...");
        ParametrosAco parametros = ParametrosAco.defaults();
        PlanificadorAco planificador = new PlanificadorAco(grafo, aeropuertoMap, parametros);

        List<String> hubs = request.getHubsInfinitos() != null && !request.getHubsInfinitos().isEmpty()
            ? request.getHubsInfinitos()
            : HUBS_DEFAULT;

        planificador.inicializar(request.getFechaInicio(), request.getFechaFin(), hubs);

        // 5. Ordenar pedidos por fecha de creación
        pedidos.sort(Comparator.comparing(T03Pedido::getT03Fechacreacion));

        // 6. Planificar cada pedido
        log.info("Iniciando planificación de {} pedidos...", pedidos.size());
        int procesados = 0;
        int asignados = 0;
        int pendientes = 0;

        for (T03Pedido pedido : pedidos) {
            try {
                // Buscar ruta usando ACO
                Ruta ruta = planificador.buscarRuta(pedido, hubs);

                if (ruta != null && ruta.esValida()) {
                    // Crear asignaciones
                    crearAsignaciones(pedido, ruta);

                    // Aplicar compromisos (actualizar capacidades)
                    planificador.aplicarCompromisos(ruta, pedido.getT03Cantidadpaquetes());

                    // Actualizar estado del pedido
                    pedido.setT03Estadoglobal("ASIGNADO");
                    pedidoRepository.save(pedido);

                    asignados++;
                } else {
                    log.warn("No se encontró ruta para pedido {}", pedido.getId());
                    pedido.setT03Estadoglobal("PENDIENTE");
                    pedidoRepository.save(pedido);
                    pendientes++;
                }

                procesados++;

                // Log cada 100 pedidos
                if (procesados % 100 == 0) {
                    log.info("Progreso: {}/{} pedidos procesados", procesados, pedidos.size());
                }

            } catch (Exception e) {
                log.error("Error procesando pedido {}: {}", pedido.getId(), e.getMessage());
                pendientes++;
                procesados++;
            }
        }

        // 7. Actualizar estadísticas de simulación
        simulacion.setPedidosProcesados(procesados);
        simulacion.setPedidosAsignados(asignados);
        simulacion.setPedidosPendientes(pendientes);

        long endTime = System.currentTimeMillis();
        simulacion.setDuracionMs(endTime - startTime);

        simulacionRepository.save(simulacion);

        log.info("Simulación completada: {}/{} pedidos asignados ({:.2f}%)",
            asignados, procesados, (asignados * 100.0 / procesados));
    }

    /**
     * Carga vuelos en el rango de fechas especificado.
     */
    private List<T06VueloProgramado> cargarVuelos(Instant fechaInicio, Instant fechaFin) {
        // Por ahora cargamos todos los vuelos
        // En producción se debería filtrar por rango de fechas
        return vueloRepository.findAll();
    }

    /**
     * Carga pedidos según el request.
     */
    private List<T03Pedido> cargarPedidos(SimulacionRequestDto request) {
        if (request.getPedidoIds() != null && !request.getPedidoIds().isEmpty()) {
            // Cargar pedidos específicos
            return pedidoRepository.findAllById(request.getPedidoIds());
        } else {
            // Cargar pedidos en el rango de fechas
            return pedidoRepository.findByT03FechacreacionBetween(
                request.getFechaInicio(),
                request.getFechaFin()
            );
        }
    }

    /**
     * Crea las asignaciones de un pedido a una ruta.
     */
    private void crearAsignaciones(T03Pedido pedido, Ruta ruta) {
        int orden = 1;
        for (Tramo tramo : ruta.getTramos()) {
            // Buscar el vuelo programado
            T06VueloProgramado vuelo = vueloRepository.findById(tramo.getVueloId())
                .orElseThrow(() -> new RuntimeException("Vuelo no encontrado: " + tramo.getVueloId()));

            T09Asignacion asignacion = T09Asignacion.builder()
                .t03Idpedido(pedido)
                .t06Idtramovuelo(vuelo)
                .t09Cantidadasignada(pedido.getT03Cantidadpaquetes())
                .t09Orden(orden++)
                .t09Estadoasignacion("CONFIRMADA")
                .build();

            asignacionRepository.save(asignacion);
        }
    }

    /**
     * Obtiene el estado actual de una simulación.
     */
    @Transactional(readOnly = true)
    public SimulacionEstadoDto obtenerEstado(Integer simulacionId) {
        T10SimulacionSemanal simulacion = simulacionRepository.findById(simulacionId)
            .orElseThrow(() -> new RuntimeException("Simulación no encontrada: " + simulacionId));

        return mapToEstadoDto(simulacion);
    }

    /**
     * Obtiene los resultados completos de una simulación.
     */
    @Transactional(readOnly = true)
    public SimulacionResultadoDto obtenerResultados(Integer simulacionId) {
        T10SimulacionSemanal simulacion = simulacionRepository.findById(simulacionId)
            .orElseThrow(() -> new RuntimeException("Simulación no encontrada: " + simulacionId));

        // Cargar asignaciones (limitadas para evitar sobrecarga)
        List<AsignacionDto> asignaciones = obtenerAsignacionesResumen(simulacionId);

        // Cargar métricas diarias
        List<MetricaDiariaDto> metricas = obtenerMetricasDiarias(simulacionId);

        // Cargar alertas
        List<AlertaDto> alertas = obtenerAlertas(simulacionId);

        // Calcular aeropuertos críticos
        List<AeropuertoCongestionDto> aeropuertosCriticos = calcularAeropuertosCriticos(simulacionId);

        return SimulacionResultadoDto.builder()
            .simulacionId(simulacion.getId())
            .estado(simulacion.getEstado())
            .fechaCreacion(simulacion.getFechaCreacion())
            .fechaInicio(simulacion.getFechaInicio())
            .fechaFin(simulacion.getFechaFin())
            .duracionMs(simulacion.getDuracionMs())
            .pedidosProcesados(simulacion.getPedidosProcesados())
            .pedidosAsignados(simulacion.getPedidosAsignados())
            .pedidosPendientes(simulacion.getPedidosPendientes())
            .tasaExito(calcularTasaExito(simulacion))
            .asignaciones(asignaciones)
            .metricasDiarias(metricas)
            .alertas(alertas)
            .aeropuertosCriticos(aeropuertosCriticos)
            .build();
    }

    /**
     * Obtiene un resumen de asignaciones (primeras 100).
     */
    private List<AsignacionDto> obtenerAsignacionesResumen(Integer simulacionId) {
        // Por ahora retornamos lista vacía
        // En producción se cargarían las primeras N asignaciones
        return new ArrayList<>();
    }

    /**
     * Obtiene métricas diarias (por ahora vacías).
     */
    private List<MetricaDiariaDto> obtenerMetricasDiarias(Integer simulacionId) {
        // Por ahora retornamos lista vacía
        // En futuras iteraciones se calcularán métricas reales
        return new ArrayList<>();
    }

    /**
     * Obtiene alertas (por ahora vacías).
     */
    private List<AlertaDto> obtenerAlertas(Integer simulacionId) {
        // Por ahora retornamos lista vacía
        return new ArrayList<>();
    }

    /**
     * Calcula aeropuertos críticos (por ahora vacío).
     */
    private List<AeropuertoCongestionDto> calcularAeropuertosCriticos(Integer simulacionId) {
        // Por ahora retornamos lista vacía
        return new ArrayList<>();
    }

    /**
     * Calcula la tasa de éxito de una simulación.
     */
    private Double calcularTasaExito(T10SimulacionSemanal simulacion) {
        if (simulacion.getPedidosProcesados() == null || simulacion.getPedidosProcesados() == 0) {
            return 0.0;
        }
        return (simulacion.getPedidosAsignados() * 100.0) / simulacion.getPedidosProcesados();
    }

    /**
     * Valida el request de simulación.
     */
    private void validarRequest(SimulacionRequestDto request) {
        if (request.getFechaInicio().isAfter(request.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        Duration duracion = Duration.between(request.getFechaInicio(), request.getFechaFin());
        if (duracion.toDays() > 30) {
            throw new IllegalArgumentException("La ventana de simulación no puede exceder 30 días");
        }
    }

    // ========== Mappers ==========

    private SimulacionResponseDto mapToResponseDto(T10SimulacionSemanal simulacion, String mensaje) {
        return SimulacionResponseDto.builder()
            .simulacionId(simulacion.getId())
            .estado(simulacion.getEstado())
            .fechaCreacion(simulacion.getFechaCreacion())
            .fechaInicio(simulacion.getFechaInicio())
            .fechaFin(simulacion.getFechaFin())
            .mensaje(mensaje)
            .build();
    }

    private SimulacionEstadoDto mapToEstadoDto(T10SimulacionSemanal simulacion) {
        double progreso = 0.0;
        if (simulacion.getPedidosProcesados() != null && simulacion.getPedidosProcesados() > 0) {
            // Estimación de progreso (simplificada)
            progreso = "COMPLETADA".equals(simulacion.getEstado()) ? 100.0 : 50.0;
        }

        return SimulacionEstadoDto.builder()
            .simulacionId(simulacion.getId())
            .estado(simulacion.getEstado())
            .fechaCreacion(simulacion.getFechaCreacion())
            .fechaInicio(simulacion.getFechaInicio())
            .fechaFin(simulacion.getFechaFin())
            .pedidosProcesados(simulacion.getPedidosProcesados())
            .pedidosAsignados(simulacion.getPedidosAsignados())
            .pedidosPendientes(simulacion.getPedidosPendientes())
            .progreso(progreso)
            .duracionMs(simulacion.getDuracionMs())
            .motivoFallo(simulacion.getMotivoFallo())
            .build();
    }
}
