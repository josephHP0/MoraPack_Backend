package com.morapack.nuevomoraback.planificacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SimulacionSemanalRequest {

    @NotNull(message = "fechaHoraInicio es requerida")
    private Instant fechaHoraInicio;

    @NotNull(message = "fechaHoraFin es requerida")
    private Instant fechaHoraFin;

    private Integer duracionBloqueHoras; // Para simulación por bloques personalizables
}
