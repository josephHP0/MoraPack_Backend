package com.morapack.nuevomoraback.planificacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class PlanificacionDiaADiaRequest {

    @NotNull(message = "fechaHoraInicio es requerida")
    private Instant fechaHoraInicio;

    private Integer ventanaHoras = 2; // Ventana por defecto: 2 horas
}
