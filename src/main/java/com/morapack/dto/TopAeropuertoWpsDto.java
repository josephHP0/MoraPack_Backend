package com.morapack.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO de aeropuerto en el ranking por WPS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopAeropuertoWpsDto {

    private Integer ranking;
    private String codigoIcao;
    private String nombreCiudad;
    private BigDecimal wpsMaximo;
}
