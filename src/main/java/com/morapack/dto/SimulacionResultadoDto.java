package com.morapack.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO con los resultados completos de una simulación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionResultadoDto {

    private Integer simulacionId;
    private String estado;
    private Instant fechaCreacion;
    private Instant fechaInicio;
    private Instant fechaFin;
    private Long duracionMs;

    /**
     * Estadísticas de pedidos.
     */
    private Integer pedidosProcesados;
    private Integer pedidosAsignados;
    private Integer pedidosPendientes;
    private Double tasaExito; // Porcentaje de pedidos asignados

    /**
     * Lista de asignaciones realizadas.
     */
    private List<AsignacionDto> asignaciones;

    /**
     * Métricas diarias de la simulación.
     */
    private List<MetricaDiariaDto> metricasDiarias;

    /**
     * Alertas generadas durante la simulación.
     */
    private List<AlertaDto> alertas;

    /**
     * Resumen de aeropuertos más congestionados.
     */
    private List<AeropuertoCongestionDto> aeropuertosCriticos;
}
