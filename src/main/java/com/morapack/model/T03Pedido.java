package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t03_pedido", schema = "morapack", indexes = {
        @Index(name = "idx_pedido_cliente", columnList = "T03_idCliente"),
        @Index(name = "idx_pedido_estado", columnList = "T03_estadoGlobal")
})
public class T03Pedido {
    @Id
    @Size(max = 32)
    @Column(name = "T03_idPedido", nullable = false, length = 32)
    private String t03Idpedido;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T03_idCliente", nullable = false)
    private T05Cliente t03Idcliente;

    @NotNull
    @Column(name = "T03_fechaCreacion", nullable = false)
    private Instant t03Fechacreacion;

    @NotNull
    @ColumnDefault("'Registrado'")
    @Lob
    @Column(name = "T03_estadoGlobal", nullable = false)
    private String t03Estadoglobal;

    @Column(name = "T03_plazoCompromiso")
    private Instant t03Plazocompromiso;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "T03_cantidadPaquetes", nullable = false)
    private Integer t03Cantidadpaquetes;

    @Column(name = "T03_fechaCreacionDestino")
    private Instant t03Fechacreaciondestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T03_idAeropuertoDestino")
    private T01Aeropuerto t03Idaeropuertodestino;

}