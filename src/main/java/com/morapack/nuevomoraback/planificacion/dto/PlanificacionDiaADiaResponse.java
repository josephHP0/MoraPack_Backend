package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanificacionDiaADiaResponse {
    private Integer pedidosPlanificados;
    private Integer pedidosRechazados;
    private List<EstadoPedidoDTO> estadoPedidos;
    private String mensaje;
}
