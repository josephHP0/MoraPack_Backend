package com.morapack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de alerta de near-collapse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaDto {

    private Integer alertaId;
    private String codigoAeropuerto;
    private String nombreCiudad;
    private Instant fechaHora;
    private BigDecimal wps;
    private Integer ocupacion;
    private Integer capacidad;
    private Double ocupacionPct;
    private String severidad; // BAJA, MEDIA, ALTA, CRITICA
    private Boolean resuelto;
}
