package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad que representa métricas agregadas de un día de simulación.
 * Incluye capacidades, WPS, rutas descartadas, pedidos y tiempos de entrega.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "t13_metrica_diaria", schema = "morapack2", indexes = {
        @Index(name = "idx_met_sim", columnList = "T10_idSimulacion"),
        @Index(name = "idx_met_fecha", columnList = "T13_fecha")
})
public class T13MetricaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T13_idMetrica", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T10_idSimulacion", nullable = false)
    private T10SimulacionSemanal simulacionSemanal;

    @NotNull
    @Column(name = "T13_fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "T13_capacidadMediaAlmacenes", precision = 5, scale = 2)
    private BigDecimal capacidadMediaAlmacenes;

    @Column(name = "T13_wpsPico95", precision = 5, scale = 2)
    private BigDecimal wpsPico95;

    @Column(name = "T13_wpsPico99", precision = 5, scale = 2)
    private BigDecimal wpsPico99;

    @ColumnDefault("0")
    @Column(name = "T13_rutasDescartadasHeadroom")
    private Integer rutasDescartadasHeadroom;

    @ColumnDefault("0")
    @Column(name = "T13_rutasDescartadasCapacidad")
    private Integer rutasDescartadasCapacidad;

    @ColumnDefault("0")
    @Column(name = "T13_pedidosEntregados")
    private Integer pedidosEntregados;

    @ColumnDefault("0")
    @Column(name = "T13_pedidosPendientes")
    private Integer pedidosPendientes;

    @Column(name = "T13_tiempoMedioEntregaHoras", precision = 6, scale = 2)
    private BigDecimal tiempoMedioEntregaHoras;

    @PrePersist
    protected void onCreate() {
        if (rutasDescartadasHeadroom == null) rutasDescartadasHeadroom = 0;
        if (rutasDescartadasCapacidad == null) rutasDescartadasCapacidad = 0;
        if (pedidosEntregados == null) pedidosEntregados = 0;
        if (pedidosPendientes == null) pedidosPendientes = 0;
    }
}
