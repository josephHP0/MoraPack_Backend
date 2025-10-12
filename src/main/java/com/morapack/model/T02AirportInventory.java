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
@Table(name = "t02_airport_inventory", schema = "morapack2")
public class T02AirportInventory {
    @EmbeddedId
    private T02AirportInventoryId id;

    @MapsId("t02Idaeropuerto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T02_idAeropuerto", nullable = false)
    private T01Aeropuerto t02Idaeropuerto;

    @MapsId("t03Idpedido")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T03_idPedido", nullable = false)
    private T03Pedido t03Idpedido;

    @NotNull
    @Column(name = "T02_cantidad", nullable = false)
    private Integer t02Cantidad;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "T02_fechaActualizacion")
    private Instant t02Fechaactualizacion;

}