package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.repository.PedidoRepository;
import com.morapack.nuevomoraback.planificacion.aco.AcoPlanner;
import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import com.morapack.nuevomoraback.planificacion.dto.EstadoPedidoDTO;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaRequest;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaResponse;
import com.morapack.nuevomoraback.planificacion.repository.T08RutaPlaneadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificadorDiaADiaServiceImpl implements PlanificadorDiaADiaService {

    private final PedidoRepository pedidoRepository;
    private final T08RutaPlaneadaRepository rutaPlaneadaRepository;
    private final AcoPlanner acoPlanner;
    private final CalculadorSLA calculadorSLA;
    private final GestorCancelaciones gestorCancelaciones;

    @Override
    @Transactional
    public PlanificacionDiaADiaResponse planificarVentana(PlanificacionDiaADiaRequest request) {

        log.info("Iniciando planificación día a día - Ventana: {} horas desde {}",
                 request.getVentanaHoras(), request.getFechaHoraInicio());

        // 1. Calcular fin de ventana
        Instant fechaFin = request.getFechaHoraInicio()
            .plusSeconds(request.getVentanaHoras() * 3600L);

        // 2. Obtener pedidos en la ventana (backlog)
        List<T02Pedido> pedidosBacklog = pedidoRepository.findPedidosNoHubBetween(
            request.getFechaHoraInicio(), fechaFin);

        log.info("Pedidos en backlog: {}", pedidosBacklog.size());

        // 3. Planificar con ACO con expansión de vuelos
        List<T08RutaPlaneada> rutasPlaneadas = acoPlanner.planificar(
            pedidosBacklog,
            T08RutaPlaneada.TipoSimulacion.DIA_A_DIA,
            request.getFechaHoraInicio(),
            fechaFin);

        // 4. Validar y guardar
        for (T08RutaPlaneada ruta : rutasPlaneadas) {
            Instant fechaEntrega = calcularFechaEntrega(ruta);
            ruta.setFechaEntregaEstimada(fechaEntrega);
            ruta.setCumpleSla(calculadorSLA.cumpleSLA(ruta.getPedido(), fechaEntrega));
        }

        rutaPlaneadaRepository.saveAll(rutasPlaneadas);

        // 5. Preparar respuesta
        int planificados = rutasPlaneadas.size();
        int rechazados = pedidosBacklog.size() - planificados;

        List<EstadoPedidoDTO> estados = rutasPlaneadas.stream()
            .map(this::convertirAEstadoDTO)
            .collect(Collectors.toList());

        return new PlanificacionDiaADiaResponse(
            planificados,
            rechazados,
            estados,
            String.format("Planificación completada: %d pedidos planificados, %d rechazados",
                         planificados, rechazados)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoPedidoDTO> consultarEstado() {
        List<T08RutaPlaneada> rutas = rutaPlaneadaRepository.findByTipoSimulacion(
            T08RutaPlaneada.TipoSimulacion.DIA_A_DIA);

        return rutas.stream()
            .map(this::convertirAEstadoDTO)
            .collect(Collectors.toList());
    }

    private Instant calcularFechaEntrega(T08RutaPlaneada ruta) {
        return ruta.getTramosAsignados().stream()
            .filter(tramo -> tramo.getEsVueloFinal())
            .findFirst()
            .map(tramo -> tramo.getVueloProgramado().getT04FechaLlegada().plusSeconds(2 * 3600))
            .orElse(Instant.now());
    }

    private EstadoPedidoDTO convertirAEstadoDTO(T08RutaPlaneada ruta) {
        return new EstadoPedidoDTO(
            ruta.getPedido().getId(),
            ruta.getPedido().getT02IdCadena(),
            ruta.getEstado().name(),
            ruta.getCumpleSla(),
            ruta.getFechaEntregaEstimada(),
            ruta.getTramosAsignados().size()
        );
    }
}
