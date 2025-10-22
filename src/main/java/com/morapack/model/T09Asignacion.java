package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "t09_asignacion", schema = "morapack2", indexes = {
        @Index(name = "idx_asig_pedido", columnList = "T03_idPedido"),
        @Index(name = "idx_asig_vuelo", columnList = "T06_idTramoVuelo")
})
public class T09Asignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T09_idAsig", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T03_idPedido", nullable = false)
    private T03Pedido t03Idpedido;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T06_idTramoVuelo", nullable = false)
    private T06VueloProgramado t06Idtramovuelo;

    @NotNull
    @Column(name = "T09_cantidadAsignada", nullable = false)
    private Integer t09Cantidadasignada;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "T09_orden", nullable = false)
    private Integer t09Orden;

    @Column(name = "T09_estadoAsignacion", nullable = false, length = 30)
    @org.hibernate.annotations.ColumnDefault("'PENDIENTE'")
    private String t09Estadoasignacion;

}