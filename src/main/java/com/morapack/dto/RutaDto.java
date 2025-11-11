package com.morapack.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO de una ruta logística.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaDto {

    private String origen;
    private String destino;
    private Instant fechaSalida;
    private Instant fechaLlegada;
    private Double duracionHoras;
    private Integer numSaltos;

    /**
     * Tramos que componen la ruta.
     */
    private List<TramoDto> tramos;

    /**
     * Itinerario en formato legible (ej: "SPIM -> SKBO -> EDDI").
     */
    private String itinerario;
}
