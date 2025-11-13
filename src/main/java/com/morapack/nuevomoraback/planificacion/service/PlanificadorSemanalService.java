package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalResponse;

public interface PlanificadorSemanalService {

    /**
     * Ejecuta simulación semanal batch
     */
    SimulacionSemanalResponse ejecutarSimulacionSemanal(SimulacionSemanalRequest request);

    /**
     * Consulta resultado de simulación por ID
     */
    SimulacionSemanalResponse consultarResultado(Integer idResultado);
}
