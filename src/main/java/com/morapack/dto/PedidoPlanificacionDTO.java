package com.morapack.dto;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PedidoPlanificacionDTO {
    private Integer id;
    private T01Aeropuerto origen;
    private T01Aeropuerto destino;
    private Integer cantidadPaquetes;
    private Instant fechaCreacion;
    private Instant fechaLimite;
    
    public static PedidoPlanificacionDTO fromPedido(T03Pedido pedido, Instant fechaLimite) {
        PedidoPlanificacionDTO dto = new PedidoPlanificacionDTO();
        dto.setId(pedido.getId());
        dto.setOrigen(pedido.getT01Idaeropuertoorigen());
        dto.setDestino(pedido.getT01Idaeropuertodestino());
        dto.setCantidadPaquetes(pedido.getT03Cantidadpaquetes());
        dto.setFechaCreacion(pedido.getT03Fechacreacion());
        dto.setFechaLimite(fechaLimite);
        return dto;
    }
}