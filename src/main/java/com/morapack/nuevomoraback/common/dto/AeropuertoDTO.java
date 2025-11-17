package com.morapack.nuevomoraback.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para información de aeropuertos
 * Usado para visualización en mapas y selección de destinos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AeropuertoDTO {

    /**
     * ID del aeropuerto
     */
    private Integer id;

    /**
     * Código ICAO del aeropuerto (ej: "SPIM", "SPJC")
     */
    private String codigoICAO;

    /**
     * Nombre/alias del aeropuerto (ej: "Lima", "Juliaca")
     */
    private String nombre;

    /**
     * Latitud (coordenada geográfica)
     */
    private BigDecimal latitud;

    /**
     * Longitud (coordenada geográfica)
     */
    private BigDecimal longitud;

    /**
     * Offset GMT (zona horaria)
     */
    private Short gmtOffset;

    /**
     * Capacidad de almacenamiento del aeropuerto
     */
    private Integer capacidad;

    /**
     * Indica si el aeropuerto es un HUB (Lima, Bruselas, Bakú)
     */
    private Boolean esHub;

    /**
     * ID de la ciudad a la que pertenece
     */
    private Integer idCiudad;
}
