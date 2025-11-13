package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPedidoDTO {
    private Integer idPedido;
    private String idCadena;
    private String estado;
    private Boolean cumpleSla;
    private Instant fechaEntregaEstimada;
    private Integer cantidadTramos;
}
