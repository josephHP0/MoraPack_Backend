package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.planificacion.dto.AssignmentByOrderDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalResponse;

import java.util.List;

public interface PlanificadorSemanalService {

    /**
     * Ejecuta simulación semanal batch
     */
    SimulacionSemanalResponse ejecutarSimulacionSemanal(SimulacionSemanalRequest request);

    /**
     * Consulta resultado de simulación por ID
     */
    SimulacionSemanalResponse consultarResultado(Integer idResultado);

    /**
     * Obtiene todas las asignaciones de rutas planeadas (para visualización en frontend)
     */
    List<AssignmentByOrderDTO> obtenerPlanificacion();
}
