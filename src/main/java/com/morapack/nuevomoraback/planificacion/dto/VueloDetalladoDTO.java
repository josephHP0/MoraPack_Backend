package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO detallado de un vuelo para el frontend.
 * Incluye información del vuelo, capacidad y pedidos asignados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloDetalladoDTO {

    // Información básica del vuelo
    private Integer vueloId;
    private String codigoOrigenICAO;
    private String nombreOrigenCiudad;
    private String codigoDestinoICAO;
    private String nombreDestinoCiudad;
    private Instant fechaSalida;
    private Instant fechaLlegada;

    // Información del avión
    private String matriculaAvion;
    private String modeloAvion;

    // Capacidad
    private Integer capacidadTotal;
    private Integer capacidadOcupada;
    private Integer capacidadDisponible;
    private Double porcentajeOcupacion;
    private String estadoCapacidad; // NORMAL, SOBRECARGA

    // Pedidos asignados a este vuelo (IDs y resumen)
    private List<PedidoEnVueloDTO> pedidosAsignados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoEnVueloDTO {
        private Integer pedidoId;
        private String idCadena;
        private Integer cantidad;
        private String destino; // Ciudad destino del pedido
        private Boolean esVueloFinal; // true si este vuelo es el último tramo del pedido
    }
}
