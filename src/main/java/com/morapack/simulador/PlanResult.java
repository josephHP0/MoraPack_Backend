package com.morapack.simulador;

import java.util.List;

public class PlanResult {
    private final List<com.morapack.model.T09Asignacion> asignaciones;

    public PlanResult(List<com.morapack.model.T09Asignacion> asignaciones) {
        this.asignaciones = asignaciones;
    }

    public List<com.morapack.model.T09Asignacion> getAsignaciones() {
        return asignaciones;
    }
}