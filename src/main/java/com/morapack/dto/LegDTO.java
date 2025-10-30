package com.morapack.dto;

import lombok.Data;

/**
 * DTO para representar un segmento de vuelo (leg) en una ruta.
 * Cada leg representa un vuelo específico en la secuencia de la ruta.
 */
@Data
public class LegDTO {
    private Integer seq;
    private String instanceId;
    private String from;
    private String to;
    private Integer qty;
}
