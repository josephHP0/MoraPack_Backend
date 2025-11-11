package com.morapack.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO de una asignación de pedido a ruta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionDto {

    private Integer asignacionId;
    private Integer pedidoId;
    private String pedidoCodigo;
    private Integer cantidadAsignada;
    private String estadoAsignacion;

    /**
     * Información de la ruta asignada.
     */
    private RutaDto ruta;
}
