package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t01_aeropuerto", schema = "morapack2")
public class T01Aeropuerto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T01_ID_AEROPUERTO", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "T01_ID_CIUDAD", nullable = false)
    private Integer t01IdCiudad;

    @Size(max = 4)
    @NotNull
    @Column(name = "T01_CODIGO_ICAO", nullable = false, length = 4)
    private String t01CodigoIcao;

    @Column(name = "T01_LAT", precision = 9, scale = 6)
    private BigDecimal t01Lat;

    @Column(name = "T01_LON", precision = 9, scale = 6)
    private BigDecimal t01Lon;

    @Column(name = "T01_GMT_OFFSET")
    private Short t01GmtOffset;

    @Column(name = "T01_CAPACIDAD")
    private Integer t01Capacidad;

    @Size(max = 40)
    @Column(name = "T01_ALIAS", length = 40)
    private String t01Alias;

}