package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de pedido con información de su planificación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDetalleDTO {

    private Integer idPedido;
    private String idCadenaPedido;
    private Instant fechaPedido;
    private Integer cantidad;
    private String destinoICAO;
    private String destinoCiudad;
    private String estado;
    private Boolean cumpleSLA;
    private Instant fechaEntregaEstimada;
}
