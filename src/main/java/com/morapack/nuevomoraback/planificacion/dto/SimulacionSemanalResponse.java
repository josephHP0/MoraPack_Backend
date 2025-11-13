package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionSemanalResponse {
    private Integer idResultado;
    private String tipoSimulacion;
    private Instant fechaInicio;
    private Instant fechaFin;
    private Instant fechaEjecucion;
    private Long duracionMs;
    private String estado;
    private String mensaje;
    private MetricasDTO metricas;
}
