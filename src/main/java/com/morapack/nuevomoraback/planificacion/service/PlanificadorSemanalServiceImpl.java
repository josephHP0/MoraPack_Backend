package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.repository.PedidoRepository;
import com.morapack.nuevomoraback.planificacion.aco.AcoPlanner;
import com.morapack.nuevomoraback.planificacion.domain.*;
import com.morapack.nuevomoraback.planificacion.dto.MetricasDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalResponse;
import com.morapack.nuevomoraback.planificacion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Override
    @Transactional
    public SimulacionSemanalResponse ejecutarSimulacionSemanal(SimulacionSemanalRequest request) {

        log.info("Iniciando simulación semanal: {} - {}",
                 request.getFechaHoraInicio(), request.getFechaHoraFin());

        Instant inicioEjecucion = Instant.now();

        // 1. Crear registro de resultado
        T10ResultadoSimulacion resultado = new T10ResultadoSimulacion();
        resultado.setTipoSimulacion(T08RutaPlaneada.TipoSimulacion.SEMANAL);
        resultado.setFechaInicio(request.getFechaHoraInicio());
        resultado.setFechaFin(request.getFechaHoraFin());
        resultado.setFechaEjecucion(inicioEjecucion);
        resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.EN_PROGRESO);
        resultado = resultadoRepository.save(resultado);

        try {
            // 2. Aplicar cancelaciones programadas
            int cancelados = gestorCancelaciones.aplicarCancelaciones(
                request.getFechaHoraInicio(), request.getFechaHoraFin());

            // 3. Cargar pedidos de la semana (excluir destinos hub)
            List<T02Pedido> pedidos = pedidoRepository.findPedidosNoHubBetween(
                request.getFechaHoraInicio(), request.getFechaHoraFin());

            log.info("Pedidos a planificar: {} (vuelos cancelados: {})", pedidos.size(), cancelados);

            // 4. Ejecutar algoritmo ACO
            List<T08RutaPlaneada> rutasPlaneadas = acoPlanner.planificar(
                pedidos, T08RutaPlaneada.TipoSimulacion.SEMANAL);

            // 5. Validar SLA y guardar rutas
            for (T08RutaPlaneada ruta : rutasPlaneadas) {
                // Calcular fecha de entrega estimada (último vuelo + 2h procesamiento)
                Instant fechaEntrega = calcularFechaEntrega(ruta);
                ruta.setFechaEntregaEstimada(fechaEntrega);

                boolean cumple = calculadorSLA.cumpleSLA(ruta.getPedido(), fechaEntrega);
                ruta.setCumpleSla(cumple);
            }

            rutaPlaneadaRepository.saveAll(rutasPlaneadas);

            // 6. Calcular métricas
            T11MetricasSimulacion metricas = calcularMetricas(rutasPlaneadas, cancelados);
            metricas.setResultadoSimulacion(resultado);
            metricasRepository.save(metricas);

            // 7. Actualizar resultado
            long duracion = Instant.now().toEpochMilli() - inicioEjecucion.toEpochMilli();
            resultado.setDuracionMs(duracion);
            resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.COMPLETADO);
            resultado.setMensaje("Simulación completada exitosamente");
            resultado.setMetricas(metricas);
            resultadoRepository.save(resultado);

            return convertirAResponse(resultado);

        } catch (Exception e) {
            log.error("Error en simulación semanal", e);
            resultado.setEstado(T10ResultadoSimulacion.EstadoSimulacion.ERROR);
            resultado.setMensaje("Error: " + e.getMessage());
            resultadoRepository.save(resultado);

            throw new RuntimeException("Error en simulación semanal", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SimulacionSemanalResponse consultarResultado(Integer idResultado) {
        T10ResultadoSimulacion resultado = resultadoRepository.findById(idResultado)
            .orElseThrow(() -> new RuntimeException("Resultado no encontrado: " + idResultado));

        return convertirAResponse(resultado);
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
