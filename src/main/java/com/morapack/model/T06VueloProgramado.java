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
@Table(name = "t06_vuelo_programado", schema = "morapack2", indexes = {
        @Index(name = "idx_vuelo_o", columnList = "T01_idAeropuertoOrigen"),
        @Index(name = "idx_vuelo_d", columnList = "T01_idAeropuertoDestino"),
        @Index(name = "idx_vuelo_avion", columnList = "T11_idAvion")
})
public class T06VueloProgramado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T06_idTramoVuelo", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuertoOrigen", nullable = false)
    private T01Aeropuerto t01Idaeropuertoorigen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuertoDestino", nullable = false)
    private T01Aeropuerto t01Idaeropuertodestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T11_idAvion")
    private T11Avion t11Idavion;

    @Column(name = "T06_fechaSalida")
    private Instant t06Fechasalida;

    @Column(name = "T06_fechaLlegada")
    private Instant t06Fechallegada;

    @Column(name = "T06_capacidadTotal")
    private Integer t06Capacidadtotal;

    @ColumnDefault("0")
    @Column(name = "T06_ocupacionActual")
    private Integer t06Ocupacionactual;

    @ColumnDefault("'PROGRAMADO'")
    @Lob
    @Column(name = "T06_estado")
    private String t06Estado;

    @ColumnDefault("'NORMAL'")
    @Lob
    @Column(name = "T06_estadoCapacidad")
    private String t06Estadocapacidad;
}