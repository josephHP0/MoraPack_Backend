package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t03_pedido", schema = "morapack2", indexes = {
        @Index(name = "idx_pedido_cliente", columnList = "T03_idCliente"),
        @Index(name = "idx_pedido_origen", columnList = "T01_idAeropuertoOrigen"),
        @Index(name = "idx_pedido_destino", columnList = "T01_idAeropuertoDestino")
})
public class T03Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T03_idPedido", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T03_idCliente", nullable = false)
    private T05Cliente t03Idcliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuertoOrigen", nullable = false)
    private T01Aeropuerto t01Idaeropuertoorigen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuertoDestino", nullable = false)
    private T01Aeropuerto t01Idaeropuertodestino;

    @NotNull
    @Column(name = "T03_cantidadPaquetes", nullable = false)
    private Integer t03Cantidadpaquetes;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "T03_fechaCreacion")
    private Instant t03Fechacreacion;

    @Column(name = "T03_plazoCompromiso")
    private Instant t03Plazocompromiso;

    @ColumnDefault("'PENDIENTE'")
    @Lob
    @Column(name = "T03_estadoGlobal")
    private String t03Estadoglobal;

}