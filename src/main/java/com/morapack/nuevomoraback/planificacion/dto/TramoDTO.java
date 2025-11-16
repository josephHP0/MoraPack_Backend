package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de un tramo individual de una ruta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TramoDTO {

    private Short ordenEnRuta;
    private Boolean esVueloFinal;
    private Integer idVuelo;
    private String origenICAO;
    private String destinoICAO;
    private Instant fechaSalida;
    private Instant fechaLlegada;
    private Integer cantidadProductos;
}
