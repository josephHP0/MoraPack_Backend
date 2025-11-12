package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t04_vuelo_programado", schema = "morapack2")
public class T04VueloProgramado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T04_ID_TRAMO_VUELO", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_ID_AEROPUERTO_ORIGEN", nullable = false)
    private T01Aeropuerto t01IdAeropuertoOrigen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_ID_AEROPUERTO_DESTINO", nullable = false)
    private T01Aeropuerto t01IdAeropuertoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T11_ID_AVION")
    private T06Avion t11IdAvion;

    @Column(name = "T04_FECHA_SALIDA")
    private Instant t04FechaSalida;

    @Column(name = "T04_FECHA_LLEGADA")
    private Instant t04FechaLlegada;

    @Column(name = "T04_CAPACIDAD_TOTAL")
    private Integer t04CapacidadTotal;

    @ColumnDefault("0")
    @Column(name = "T04_OCUPACION_TOTAL")
    private Integer t04OcupacionTotal;

    @ColumnDefault("'PROGRAMADO'")
    @Lob
    @Column(name = "T04_ESTADO")
    private String t04Estado;

    @ColumnDefault("'NORMAL'")
    @Lob
    @Column(name = "T04_ESTADO_CAPACIDAD")
    private String t04EstadoCapacidad;

}