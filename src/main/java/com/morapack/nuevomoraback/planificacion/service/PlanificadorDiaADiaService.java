package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.planificacion.dto.EstadoPedidoDTO;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaRequest;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaResponse;

import java.util.List;

public interface PlanificadorDiaADiaService {

    /**
     * Planifica pedidos en ventana corta (1-2 horas)
     */
    PlanificacionDiaADiaResponse planificarVentana(PlanificacionDiaADiaRequest request);

    /**
     * Consulta estado actual del backlog
     */
    List<EstadoPedidoDTO> consultarEstado();
}
