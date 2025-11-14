package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO para debug - información sobre pedidos disponibles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebugPedidosDTO {

    private Instant fechaInicio;
    private Instant fechaFin;

    // Contadores
    private Integer totalPedidosEnBD;
    private Integer pedidosEnRango;
    private Integer pedidosNoHub;
    private Integer pedidosHubExcluidos;

    // Distribución por fecha
    private List<PedidosPorDia> distribucionPorDia;

    // Muestras
    private List<PedidoSimpleDTO> muestraPedidos; // Primeros 10 pedidos

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidosPorDia {
        private String fecha; // "2025-11-13"
        private Integer cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoSimpleDTO {
        private Integer id;
        private String idCadena;
        private Instant fechaPedido;
        private Integer cantidad;
        private String destino;
        private Boolean esHub;
    }
}
