package com.morapack.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de métricas diarias de simulación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricaDiariaDto {

    private Integer metricaId;
    private LocalDate fecha;

    // Métricas de capacidad
    private BigDecimal capacidadMediaAlmacenes;
    private BigDecimal wpsPico95;
    private BigDecimal wpsPico99;

    // Métricas de rutas
    private Integer rutasDescartadasHeadroom;
    private Integer rutasDescartadasCapacidad;

    // Métricas de pedidos
    private Integer pedidosEntregados;
    private Integer pedidosPendientes;
    private BigDecimal tiempoMedioEntregaHoras;

    // Top aeropuertos por WPS
    private List<TopAeropuertoWpsDto> topAeropuertos;
}
