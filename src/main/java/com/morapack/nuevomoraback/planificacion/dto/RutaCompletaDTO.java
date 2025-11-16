package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO de ruta completa con todos sus tramos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaCompletaDTO {

    private Integer idRuta;
    private Integer idPedido;
    private String estadoRuta;
    private Boolean cumpleSLA;
    private Instant fechaEntregaEstimada;
    private List<TramoDTO> tramos;
}
