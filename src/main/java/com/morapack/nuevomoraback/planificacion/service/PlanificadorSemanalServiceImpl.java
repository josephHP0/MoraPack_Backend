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
        validarSLA(rutas);
        rutaPlaneadaRepository.saveAll(rutas);
    }

    /**
     * Valida SLA sin guardar (para conversión a DTO antes de persistir)
     */
    private void validarSLA(List<T08RutaPlaneada> rutas) {
        for (T08RutaPlaneada ruta : rutas) {
            Instant fechaEntrega = calcularFechaEntrega(ruta);
            ruta.setFechaEntregaEstimada(fechaEntrega);

            boolean cumple = calculadorSLA.cumpleSLA(ruta.getPedido(), fechaEntrega);
            ruta.setCumpleSla(cumple);
        }
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
     * Calcula el número de bloque actual basándose en la fecha de inicio de la simulación
     * y la fecha de inicio del bloque actual
     */
    private Integer calcularNumeroBloqueActual(Instant inicioSimulacion, Instant inicioBloqueActual, Integer duracionBloqueHoras) {
        long segundosTranscurridos = inicioBloqueActual.getEpochSecond() - inicioSimulacion.getEpochSecond();
        long duracionBloqueSegundos = duracionBloqueHoras * 3600L;
        
        // El primer bloque es 1, no 0
        int numeroBloque = (int) (segundosTranscurridos / duracionBloqueSegundos) + 1;
        
        return numeroBloque;
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

    @Override
    @Transactional
    public BloqueSimulacionResponse procesarBloqueIncremental(BloqueSimulacionRequest request) {
        log.info("========================================");
        log.info("BLOQUE INCREMENTAL");
        log.info("Rango: {} a {}", request.getFechaInicio(), request.getFechaFin());
        log.info("ID Simulación: {}", request.getIdResultadoSimulacion());
        log.info("Último bloque: {}", request.getEsUltimoBloque());
        log.info("========================================");

        long tiempoInicio = System.currentTimeMillis();

        // 1. Crear o recuperar resultado de simulación
        T10ResultadoSimulacion resultado = obtenerOCrearResultado(request);

        // 2. Cargar pedidos del bloque
        List<T02Pedido> pedidosBloque = pedidoRepository.findPedidosNoHubBetween(
            request.getFechaInicio(),
            request.getFechaFin()
        );

        log.info("Pedidos en este bloque: {}", pedidosBloque.size());

        List<T08RutaPlaneada> rutasBloque = new ArrayList<>();
        BloqueSimulacionResponse response = null;
        long tiempoProcesamiento;

        if (!pedidosBloque.isEmpty()) {
            // 3. Ejecutar ACO para este bloque específico
            rutasBloque = acoPlanner.planificar(
                pedidosBloque,
                T08RutaPlaneada.TipoSimulacion.SEMANAL,
                request.getFechaInicio(),
                request.getFechaFin()
            );

            // 4. Validar SLA (sin guardar todavía)
            validarSLA(rutasBloque);

            // 5. Calcular métricas del bloque (antes de guardar)
            tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;
            MetricasBloqueDTO metricas = calcularMetricasBloque(rutasBloque, pedidosBloque.size(), tiempoProcesamiento);

            // 6. Calcular número de bloque actual
            // Usamos 3 horas como duración estándar de bloque (K=3)
            Integer numeroBloque = calcularNumeroBloqueActual(
                resultado.getFechaInicio(),
                request.getFechaInicio(),
                3  // K = 3 horas (valor estándar del sistema)
            );
            log.info("Número de bloque calculado: {}", numeroBloque);

            // 7. Convertir a DTO ANTES de guardar (mientras todo está en memoria)
            log.info("Convirtiendo rutas a DTO antes de persistir...");
            response = conversorSimulacionService.convertirABloqueResponse(
                resultado.getId(),
                request.getFechaInicio(),
                request.getFechaFin(),
                rutasBloque,
                metricas,
                numeroBloque
            );
            log.info("Conversión completada, procediendo a guardar en BD...");

            // 7. Ahora sí guardar en BD
            rutaPlaneadaRepository.saveAll(rutasBloque);
            log.info("{} rutas guardadas en BD", rutasBloque.size());
        } else {
            // Sin pedidos, crear response vacío
            tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;
            MetricasBloqueDTO metricas = MetricasBloqueDTO.builder()
                .totalPedidosProcesados(0)
                .pedidosAsignados(0)
                .pedidosNoAsignados(0)
                .vuelosUtilizados(0)
                .capacidadOcupada(0)
                .tiempoProcesamiento(tiempoProcesamiento)
                .build();

            // Calcular número de bloque para respuesta vacía también
            Integer numeroBloque = calcularNumeroBloqueActual(
                resultado.getFechaInicio(),
                request.getFechaInicio(),
                3  // K = 3 horas (valor estándar del sistema)
            );

            response = BloqueSimulacionResponse.builder()
                .idResultadoSimulacion(resultado.getId())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .numeroBloque(numeroBloque)
                .vuelos(new ArrayList<>())
                .pedidos(new ArrayList<>())
                .rutas(new ArrayList<>())
                .metricas(metricas)
                .hayMasBloques(false)
                .build();
        }

        // 8. Marcar como completado si es el último bloque
        if (Boolean.TRUE.equals(request.getEsUltimoBloque())) {
            resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.COMPLETADO);
            resultado.setMensaje("Simulación incremental completada");
            resultadoRepository.save(resultado);
            log.info("Último bloque procesado - Simulación COMPLETADA");
        }

        // 9. Calcular sugerencia para el siguiente bloque
        // Sugerir solicitar el siguiente bloque cuando falte 20% del tiempo actual
        if (response != null) {
            long duracionBloqueSegundos = request.getFechaFin().getEpochSecond() - request.getFechaInicio().getEpochSecond();
            long sugerenciaOffset = (long) (duracionBloqueSegundos * 0.8);
            response.setSugerenciaSolicitudSiguienteBloque(
                request.getFechaInicio().plusSeconds(sugerenciaOffset)
            );

            response.setHayMasBloques(!Boolean.TRUE.equals(request.getEsUltimoBloque()));
        }

        log.info("Bloque procesado: {} rutas en {}ms", rutasBloque.size(), tiempoProcesamiento);
        log.info("Response generado correctamente, enviando al cliente...");
        log.info("========================================");

        return response;
    }

    /**
     * Obtiene un resultado de simulación existente o crea uno nuevo
     */
    private T10ResultadoSimulacion obtenerOCrearResultado(BloqueSimulacionRequest request) {
        if (request.getIdResultadoSimulacion() != null) {
            // Recuperar resultado existente
            return resultadoRepository.findById(request.getIdResultadoSimulacion())
                .orElseThrow(() -> new RuntimeException("Resultado no encontrado: " + request.getIdResultadoSimulacion()));
        } else {
            // Crear nuevo resultado para esta simulación incremental
            T10ResultadoSimulacion nuevoResultado = new T10ResultadoSimulacion();
            nuevoResultado.setTipoSimulacion(T08RutaPlaneada.TipoSimulacion.SEMANAL);
            nuevoResultado.setFechaInicio(request.getFechaInicio());
            nuevoResultado.setFechaFin(request.getFechaFin());
            nuevoResultado.setFechaEjecucion(Instant.now());
            nuevoResultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.EN_PROGRESO);
            nuevoResultado.setMensaje("Simulación incremental iniciada");
            return resultadoRepository.save(nuevoResultado);
        }
    }

    /**
     * Calcula métricas específicas de un bloque
     */
    private MetricasBloqueDTO calcularMetricasBloque(List<T08RutaPlaneada> rutas, int totalPedidos, long tiempoProcesamiento) {
        int pedidosAsignados = rutas.size();
        int pedidosNoAsignados = totalPedidos - pedidosAsignados;

        int vuelosUtilizados = (int) rutas.stream()
            .flatMap(r -> r.getTramosAsignados().stream())
            .map(t -> t.getVueloProgramado().getId())
            .distinct()
            .count();

        int capacidadOcupada = rutas.stream()
            .flatMap(r -> r.getTramosAsignados().stream())
            .mapToInt(T09TramoAsignado::getCantidadProductos)
            .sum();

        return MetricasBloqueDTO.builder()
            .totalPedidosProcesados(totalPedidos)
            .pedidosAsignados(pedidosAsignados)
            .pedidosNoAsignados(pedidosNoAsignados)
            .vuelosUtilizados(vuelosUtilizados)
            .capacidadOcupada(capacidadOcupada)
            .tiempoProcesamiento(tiempoProcesamiento)
            .build();
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
