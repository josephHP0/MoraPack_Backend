package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.planificacion.dto.DebugPedidosDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionDetalladaDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;

import java.time.Instant;

public interface PlanificadorSemanalService {

    /**
     * Ejecuta simulación semanal batch (con o sin bloques)
     * Si duracionBloqueHoras es null, ejecuta todo de golpe
     * Si tiene valor, divide la simulación en bloques del tamaño especificado
     */
    SimulacionDetalladaDTO ejecutarSimulacionSemanal(SimulacionSemanalRequest request);

    /**
     * Consulta resultado de simulación por ID con información detallada
     */
    SimulacionDetalladaDTO consultarResultado(Integer idResultado);

    /**
     * Debug - Verifica cuántos pedidos hay disponibles en un rango de fechas
     */
    DebugPedidosDTO debugPedidosDisponibles(Instant fechaInicio, Instant fechaFin);
}
