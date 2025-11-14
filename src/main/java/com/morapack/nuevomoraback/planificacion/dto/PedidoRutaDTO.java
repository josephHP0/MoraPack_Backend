package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO detallado de un pedido con su ruta completa.
 * Para visualización en el frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRutaDTO {

    // Información del pedido
    private Integer pedidoId;
    private String idCadena;
    private Instant fechaPedido;
    private Integer cantidad;
    private String clienteNombre;

    // Destino final
    private String destinoCodigoICAO;
    private String destinoCiudad;

    // Estado de la planificación
    private String estado; // PENDIENTE, EN_TRANSITO, ENTREGADO, RECHAZADO
    private Boolean cumpleSla;
    private Instant fechaEntregaEstimada;
    private Instant fechaLimiteSla;

    // Ruta completa (lista de tramos en orden)
    private List<TramoRutaDTO> tramos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TramoRutaDTO {
        private Integer ordenEnRuta; // 1, 2, 3...
        private Integer vueloId;
        private String origenICAO;
        private String origenCiudad;
        private String destinoICAO;
        private String destinoCiudad;
        private Instant fechaSalida;
        private Instant fechaLlegada;
        private Boolean esVueloFinal;
        private Integer cantidadProductos;
    }
}
