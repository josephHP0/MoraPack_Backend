package com.morapack.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de resumen de congestión de un aeropuerto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AeropuertoCongestionDto {

    private String codigoIcao;
    private String nombreCiudad;
    private BigDecimal wpsMaximo;
    private Integer numeroAlertas;
    private String severidadMaxima;
}
