package com.morapack.nuevomoraback.planificacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * Request para solicitar un bloque específico de simulación.
 * Permite al front solicitar datos incrementalmente según avanza la visualización.
 */
@Data
public class BloqueSimulacionRequest {

    @NotNull(message = "fechaInicio del bloque es requerida")
    private Instant fechaInicio;

    @NotNull(message = "fechaFin del bloque es requerida")
    private Instant fechaFin;

    /**
     * ID de resultado de simulación existente (opcional).
     * Si se proporciona, vincula este bloque a una simulación en progreso.
     */
    private Integer idResultadoSimulacion;

    /**
     * Indica si este es el último bloque de la simulación.
     * El back usará esto para marcar el resultado como COMPLETADO.
     */
    private Boolean esUltimoBloque = false;
}
