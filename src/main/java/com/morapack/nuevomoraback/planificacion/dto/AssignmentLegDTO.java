package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentLegDTO {
    private Integer seq;           // Secuencia del tramo (orden)
    private String instanceId;     // ID del vuelo programado
    private String from;           // ICAO código de origen
    private String to;             // ICAO código de destino
    private Integer qty;           // Cantidad de productos en este tramo
}
