package com.morapack.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO de un tramo de ruta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TramoDto {

    private Integer vueloId;
    private String instanceId;
    private String origen;
    private String destino;
    private Instant horaSalida;
    private Instant horaLlegada;
    private Double duracionHoras;
}
