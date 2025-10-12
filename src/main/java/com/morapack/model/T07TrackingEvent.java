package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t07_tracking_event", schema = "morapack2", indexes = {
        @Index(name = "idx_track_pedido", columnList = "T03_idPedido")
})
public class T07TrackingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T07_idEvento", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T03_idPedido", nullable = false)
    private T03Pedido t03Idpedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T06_idTramoVuelo")
    private T06VueloProgramado t06Idtramovuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T01_idAeropuerto")
    private T01Aeropuerto t01Idaeropuerto;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "T07_timestamp")
    private Instant t07Timestamp;

    @Lob
    @Column(name = "T07_estadoOficial")
    private String t07Estadooficial;

    @Column(name = "T07_lat", precision = 9, scale = 6)
    private BigDecimal t07Lat;

    @Column(name = "T07_lon", precision = 9, scale = 6)
    private BigDecimal t07Lon;

}