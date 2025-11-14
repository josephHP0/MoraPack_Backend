package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.repository.PedidoRepository;
import com.morapack.nuevomoraback.planificacion.aco.AcoPlanner;
import com.morapack.nuevomoraback.planificacion.domain.*;
import com.morapack.nuevomoraback.planificacion.dto.*;
import com.morapack.nuevomoraback.planificacion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificadorSemanalServiceImpl implements PlanificadorSemanalService {

    private final PedidoRepository pedidoRepository;
    private final T08RutaPlaneadaRepository rutaPlaneadaRepository;
    private final T10ResultadoSimulacionRepository resultadoRepository;
    private final T11MetricasSimulacionRepository metricasRepository;
    private final AcoPlanner acoPlanner;
    private final ValidadorReglas validadorReglas;
    private final CalculadorSLA calculadorSLA;
    private final GestorCancelaciones gestorCancelaciones;
    private final ConversorSimulacionService conversorSimulacionService;

    @Override
    @Transactional
    public SimulacionDetalladaDTO ejecutarSimulacionSemanal(SimulacionSemanalRequest request) {

        log.info("========================================");
        log.info("Iniciando simulación semanal");
        log.info("Periodo: {} a {}", request.getFechaHoraInicio(), request.getFechaHoraFin());
        log.info("Duración bloque: {} horas", request.getDuracionBloqueHoras() != null ? request.getDuracionBloqueHoras() : "N/A (batch completo)");
        log.info("========================================");

        Instant inicioEjecucion = Instant.now();

        // 1. Crear registro de resultado
        T10ResultadoSimulacion resultado = new T10ResultadoSimulacion();
        resultado.setTipoSimulacion(T08RutaPlaneada.TipoSimulacion.SEMANAL);
        resultado.setFechaInicio(request.getFechaHoraInicio());
        resultado.setFechaFin(request.getFechaHoraFin());
        resultado.setFechaEjecucion(inicioEjecucion);
        resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.EN_PROGRESO);
        resultado = resultadoRepository.save(resultado);

        Integer numeroBloquesEjecutados = 0;
        List<T08RutaPlaneada> todasLasRutas = new ArrayList<>();

        try {
            // 2. Aplicar cancelaciones programadas
            int cancelados = gestorCancelaciones.aplicarCancelaciones(
                request.getFechaHoraInicio(), request.getFechaHoraFin());

            // 3. Decidir estrategia: con bloques o sin bloques
            if (request.getDuracionBloqueHoras() != null && request.getDuracionBloqueHoras() > 0) {
                // ========== SIMULACIÓN POR BLOQUES ==========
                todasLasRutas = ejecutarSimulacionPorBloques(request, cancelados);
                numeroBloquesEjecutados = calcularNumeroBloquesTotales(request);
            } else {
                // ========== SIMULACIÓN BATCH COMPLETA ==========
                todasLasRutas = ejecutarSimulacionBatch(request);
                numeroBloquesEjecutados = 1;
            }

            // 4. Calcular métricas
            T11MetricasSimulacion metricas = calcularMetricas(todasLasRutas, cancelados);
            metricas.setResultadoSimulacion(resultado);
            metricasRepository.save(metricas);

            // 5. Actualizar resultado
            long duracion = Instant.now().toEpochMilli() - inicioEjecucion.toEpochMilli();
            resultado.setDuracionMs(duracion);
            resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.COMPLETADO);
            resultado.setMensaje(String.format("Simulación completada: %d bloques, %d pedidos planificados",
                                              numeroBloquesEjecutados, todasLasRutas.size()));
            resultado.setMetricas(metricas);
            resultadoRepository.save(resultado);

            // 6. Convertir a DTO detallado
            return conversorSimulacionService.convertirADetallado(
                resultado,
                todasLasRutas,
                request.getDuracionBloqueHoras(),
                numeroBloquesEjecutados
            );

        } catch (Exception e) {
            log.error("Error en simulación semanal", e);
            resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.ERROR);
            resultado.setMensaje("Error: " + e.getMessage());
            resultadoRepository.save(resultado);

            throw new RuntimeException("Error en simulación semanal", e);
        }
    }

    /**
     * Ejecuta simulación batch completa (todo de golpe)
     */
    private List<T08RutaPlaneada> ejecutarSimulacionBatch(SimulacionSemanalRequest request) {
        log.info("Ejecutando simulación BATCH (sin bloques)");

        // Cargar todos los pedidos del periodo
        List<T02Pedido> pedidos = pedidoRepository.findPedidosNoHubBetween(
            request.getFechaHoraInicio(), request.getFechaHoraFin());

        logInfoPedidos(pedidos, request.getFechaHoraInicio(), request.getFechaHoraFin());

        // Ejecutar ACO una sola vez con todos los pedidos
        List<T08RutaPlaneada> rutas = acoPlanner.planificar(
            pedidos,
            T08RutaPlaneada.TipoSimulacion.SEMANAL,
            request.getFechaHoraInicio(),
            request.getFechaHoraFin()
        );

        // Validar SLA y guardar
        validarYGuardarRutas(rutas);

        return rutas;
    }

    /**
     * Ejecuta simulación dividida en bloques
     */
    private List<T08RutaPlaneada> ejecutarSimulacionPorBloques(SimulacionSemanalRequest request, int cancelados) {
        log.info("Ejecutando simulación POR BLOQUES de {} horas", request.getDuracionBloqueHoras());

        List<T08RutaPlaneada> todasLasRutas = new ArrayList<>();

        Instant inicioBloque = request.getFechaHoraInicio();
        Instant finTotal = request.getFechaHoraFin();
        long duracionBloqueSegundos = request.getDuracionBloqueHoras() * 3600L;

        int bloqueNumero = 1;

        while (inicioBloque.isBefore(finTotal)) {
            // Calcular fin de este bloque
            Instant finBloque = inicioBloque.plusSeconds(duracionBloqueSegundos);
            if (finBloque.isAfter(finTotal)) {
                finBloque = finTotal;
            }

            log.info("========================================");
            log.info("Bloque {}: {} a {}", bloqueNumero, inicioBloque, finBloque);
            log.info("========================================");

            // Cargar pedidos de este bloque
            List<T02Pedido> pedidosBloque = pedidoRepository.findPedidosNoHubBetween(inicioBloque, finBloque);
            log.info("Pedidos en bloque: {}", pedidosBloque.size());

            if (!pedidosBloque.isEmpty()) {
                // Ejecutar ACO para este bloque
                List<T08RutaPlaneada> rutasBloque = acoPlanner.planificar(
                    pedidosBloque,
                    T08RutaPlaneada.TipoSimulacion.SEMANAL,
                    inicioBloque,
                    finBloque
                );

                // Validar SLA y guardar
                validarYGuardarRutas(rutasBloque);

                todasLasRutas.addAll(rutasBloque);
                log.info("Bloque {} completado: {} rutas planificadas", bloqueNumero, rutasBloque.size());
            }

            // Avanzar al siguiente bloque
            inicioBloque = finBloque;
            bloqueNumero++;
        }

        log.info("========================================");
        log.info("Simulación por bloques completada: {} bloques, {} rutas totales",
                 bloqueNumero - 1, todasLasRutas.size());
        log.info("========================================");

        return todasLasRutas;
    }

    /**
     * Valida SLA y guarda rutas
     */
    private void validarYGuardarRutas(List<T08RutaPlaneada> rutas) {
        for (T08RutaPlaneada ruta : rutas) {
            Instant fechaEntrega = calcularFechaEntrega(ruta);
            ruta.setFechaEntregaEstimada(fechaEntrega);

            boolean cumple = calculadorSLA.cumpleSLA(ruta.getPedido(), fechaEntrega);
            ruta.setCumpleSla(cumple);
        }

        rutaPlaneadaRepository.saveAll(rutas);
    }

    /**
     * Calcula número total de bloques
     */
    private Integer calcularNumeroBloquesTotales(SimulacionSemanalRequest request) {
        long duracionTotalSegundos = request.getFechaHoraFin().getEpochSecond() -
                                     request.getFechaHoraInicio().getEpochSecond();
        long duracionBloqueSegundos = request.getDuracionBloqueHoras() * 3600L;

        return (int) Math.ceil((double) duracionTotalSegundos / duracionBloqueSegundos);
    }

    /**
     * Log información de pedidos
     */
    private void logInfoPedidos(List<T02Pedido> pedidos, Instant inicio, Instant fin) {
        log.info("========================================");
        log.info("Query de pedidos:");
        log.info("  Rango: {} a {}", inicio, fin);
        log.info("  Pedidos encontrados: {}", pedidos.size());

        if (pedidos.isEmpty()) {
            log.warn("ADVERTENCIA: No se encontraron pedidos en el rango especificado");
            log.warn("Verificar:");
            log.warn("  1. Que existan pedidos en la tabla T02_PEDIDO");
            log.warn("  2. Que las fechas coincidan con T02_FECHA_PEDIDO");
            log.warn("  3. Que los destinos NO sean hubs (Lima/Bruselas/Bakú)");
        } else {
            T02Pedido primero = pedidos.get(0);
            T02Pedido ultimo = pedidos.get(pedidos.size() - 1);
            log.info("  Primer pedido: ID={}, Fecha={}", primero.getId(), primero.getT02FechaPedido());
            log.info("  Último pedido: ID={}, Fecha={}", ultimo.getId(), ultimo.getT02FechaPedido());
        }
        log.info("========================================");
    }

    @Override
    @Transactional(readOnly = true)
    public SimulacionDetalladaDTO consultarResultado(Integer idResultado) {
        T10ResultadoSimulacion resultado = resultadoRepository.findById(idResultado)
            .orElseThrow(() -> new RuntimeException("Resultado no encontrado: " + idResultado));

        // Cargar todas las rutas planificadas de esta simulación
        List<T08RutaPlaneada> rutas = rutaPlaneadaRepository.findByTipoSimulacion(
            T08RutaPlaneada.TipoSimulacion.SEMANAL
        );

        // TODO: Filtrar rutas por resultado específico (requiere agregar campo en T08)
        // Por ahora retorna todas las rutas de tipo SEMANAL

        return conversorSimulacionService.convertirADetallado(
            resultado,
            rutas,
            null, // No sabemos duracionBloqueHoras del histórico
            null  // No sabemos numeroBloquesEjecutados del histórico
        );
    }

    private Instant calcularFechaEntrega(T08RutaPlaneada ruta) {
        return ruta.getTramosAsignados().stream()
            .filter(T09TramoAsignado::getEsVueloFinal)
            .findFirst()
            .map(tramo -> tramo.getVueloProgramado().getT04FechaLlegada().plusSeconds(2 * 3600))
            .orElse(Instant.now());
    }

    private T11MetricasSimulacion calcularMetricas(List<T08RutaPlaneada> rutas, int cancelados) {
        T11MetricasSimulacion metricas = new T11MetricasSimulacion();

        metricas.setTotalPedidos(rutas.size());
        metricas.setPedidosEntregados((int) rutas.stream()
            .filter(r -> r.getEstado() == T08RutaPlaneada.EstadoRuta.ENTREGADO)
            .count());
        metricas.setPedidosEnTransito((int) rutas.stream()
            .filter(r -> r.getEstado() == T08RutaPlaneada.EstadoRuta.EN_TRANSITO)
            .count());
        metricas.setPedidosRechazados((int) rutas.stream()
            .filter(r -> r.getEstado() == T08RutaPlaneada.EstadoRuta.RECHAZADO)
            .count());

        long cumpleSla = rutas.stream().filter(T08RutaPlaneada::getCumpleSla).count();
        metricas.setCumplimientoSla(
            java.math.BigDecimal.valueOf(100.0 * cumpleSla / Math.max(1, rutas.size())));

        metricas.setVuelosCancelados(cancelados);
        metricas.setVuelosUtilizados((int) rutas.stream()
            .flatMap(r -> r.getTramosAsignados().stream())
            .map(t -> t.getVueloProgramado().getId())
            .distinct()
            .count());

        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public DebugPedidosDTO debugPedidosDisponibles(Instant fechaInicio, Instant fechaFin) {
        log.info("========================================");
        log.info("DEBUG - Verificando pedidos disponibles");
        log.info("Rango: {} a {}", fechaInicio, fechaFin);
        log.info("========================================");

        DebugPedidosDTO debug = new DebugPedidosDTO();
        debug.setFechaInicio(fechaInicio);
        debug.setFechaFin(fechaFin);

        // 1. Total de pedidos en BD
        long totalPedidos = pedidoRepository.count();
        debug.setTotalPedidosEnBD((int) totalPedidos);
        log.info("Total pedidos en BD: {}", totalPedidos);

        // 2. Pedidos en el rango (todos, incluye hubs)
        List<T02Pedido> pedidosEnRango = pedidoRepository.findByFechaPedidoBetween(fechaInicio, fechaFin);
        debug.setPedidosEnRango(pedidosEnRango.size());
        log.info("Pedidos en rango (incluye hubs): {}", pedidosEnRango.size());

        // 3. Pedidos NO-HUB (los que se usan para planificación)
        List<T02Pedido> pedidosNoHub = pedidoRepository.findPedidosNoHubBetween(fechaInicio, fechaFin);
        debug.setPedidosNoHub(pedidosNoHub.size());
        debug.setPedidosHubExcluidos(pedidosEnRango.size() - pedidosNoHub.size());
        log.info("Pedidos NO-HUB (planificables): {}", pedidosNoHub.size());
        log.info("Pedidos HUB excluidos: {}", debug.getPedidosHubExcluidos());

        // 4. Muestra de primeros 10 pedidos
        List<DebugPedidosDTO.PedidoSimpleDTO> muestra = pedidosNoHub.stream()
            .limit(10)
            .map(p -> new DebugPedidosDTO.PedidoSimpleDTO(
                p.getId(),
                p.getT02IdCadena(),
                p.getT02FechaPedido(),
                p.getT02Cantidad(),
                p.getT02IdAeropDestino().getT01Alias(),
                false  // Todos son NO-HUB porque vienen del query findPedidosNoHubBetween
            ))
            .toList();
        debug.setMuestraPedidos(muestra);

        // 5. Distribución por día (simplificado - agrupar por día)
        java.util.Map<String, Long> pedidosPorDia = pedidosNoHub.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                p -> p.getT02FechaPedido().atZone(java.time.ZoneId.of("UTC")).toLocalDate().toString(),
                java.util.stream.Collectors.counting()
            ));

        List<DebugPedidosDTO.PedidosPorDia> distribucion = pedidosPorDia.entrySet().stream()
            .map(e -> new DebugPedidosDTO.PedidosPorDia(e.getKey(), e.getValue().intValue()))
            .sorted(java.util.Comparator.comparing(DebugPedidosDTO.PedidosPorDia::getFecha))
            .toList();
        debug.setDistribucionPorDia(distribucion);

        log.info("Distribución por día:");
        distribucion.forEach(d -> log.info("  {}: {} pedidos", d.getFecha(), d.getCantidad()));

        log.info("========================================");

        return debug;
    }

    private SimulacionSemanalResponse convertirAResponse(T10ResultadoSimulacion resultado) {
        MetricasDTO metricasDTO = null;

        if (resultado.getMetricas() != null) {
            T11MetricasSimulacion m = resultado.getMetricas();
            metricasDTO = new MetricasDTO(
                m.getTotalPedidos(),
                m.getPedidosEntregados(),
                m.getPedidosEnTransito(),
                m.getPedidosRechazados(),
                m.getCumplimientoSla(),
                m.getOcupacionPromedio(),
                m.getVuelosUtilizados(),
                m.getVuelosCancelados()
            );
        }

        return new SimulacionSemanalResponse(
            resultado.getId(),
            resultado.getTipoSimulacion().name(),
            resultado.getFechaInicio(),
            resultado.getFechaFin(),
            resultado.getFechaEjecucion(),
            resultado.getDuracionMs(),
            resultado.getEstado().name(),
            resultado.getMensaje(),
            metricasDTO
        );
    }
}
