package com.morapack.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO con el estado de una simulación en ejecución.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionEstadoDto {

    private Integer simulacionId;
    private String estado;
    private Instant fechaCreacion;
    private Instant fechaInicio;
    private Instant fechaFin;

    /**
     * Número de pedidos procesados hasta el momento.
     */
    private Integer pedidosProcesados;

    /**
     * Número de pedidos con ruta asignada exitosamente.
     */
    private Integer pedidosAsignados;

    /**
     * Número de pedidos sin ruta factible.
     */
    private Integer pedidosPendientes;

    /**
     * Porcentaje de progreso (0-100).
     */
    private Double progreso;

    /**
     * Duración de la simulación en milisegundos (solo si completada).
     */
    private Long duracionMs;

    /**
     * Motivo de fallo (solo si estado=FALLIDA).
     */
    private String motivoFallo;
}
