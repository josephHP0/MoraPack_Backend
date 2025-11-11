package com.morapack.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO de respuesta al iniciar una simulación semanal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionResponseDto {

    /**
     * ID único de la simulación creada.
     */
    private Integer simulacionId;

    /**
     * Estado actual de la simulación.
     * Valores: EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA
     */
    private String estado;

    /**
     * Fecha y hora de creación de la simulación.
     */
    private Instant fechaCreacion;

    /**
     * Fecha y hora de inicio de la ventana de simulación.
     */
    private Instant fechaInicio;

    /**
     * Fecha y hora de fin de la ventana de simulación.
     */
    private Instant fechaFin;

    /**
     * Mensaje descriptivo del estado.
     */
    private String mensaje;
}
