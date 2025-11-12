package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t07_rutas", schema = "morapack2")
public class T07Ruta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T07_ID_RUTA", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T01_ID_AERO_ORIGEN")
    private T01Aeropuerto t01IdAeroOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T01_ID_AERO_DESTINO")
    private T01Aeropuerto t01IdAeroDestino;

    @Column(name = "T07_SLA", precision = 10, scale = 3)
    private BigDecimal t07Sla;

    @Column(name = "T07_FECHA_SALIDA")
    private Instant t07FechaSalida;

    @Column(name = "T07_FECHA_LLEGADA")
    private Instant t07FechaLlegada;

    @Size(max = 1000)
    @Column(name = "T07_RUTA_PLANEADA", length = 1000)
    private String t07RutaPlaneada;

}