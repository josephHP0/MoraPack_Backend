package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t01_aeropuerto", schema = "morapack", indexes = {
        @Index(name = "idx_t01_fk_ciudad", columnList = "T01_idCiudad"),
        @Index(name = "idx_t01_pais", columnList = "T01_pais"),
        @Index(name = "idx_t01_cont_gmt", columnList = "T01_continente, T01_gmt_offset")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_t01_icao", columnNames = {"T01_codigoICAO"})
})
public class T01Aeropuerto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T01_idAeropuerto", nullable = false)
    private Integer t0Idaeropuerto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idCiudad", nullable = false)
    private T08Ciudad t01Idciudad;

    @Size(max = 4)
    @NotNull
    @Column(name = "T01_codigoICAO", nullable = false, length = 4)
    private String t01Codigoicao;

    @Size(max = 80)
    @NotNull
    @Column(name = "T01_pais", nullable = false, length = 80)
    private String t01Pais;

    @Size(max = 10)
    @Column(name = "T01_alias", length = 10)
    private String t01Alias;

    @Size(max = 40)
    @NotNull
    @Column(name = "T01_continente", nullable = false, length = 40)
    private String t01Continente;

    @NotNull
    @Column(name = "T01_gmt_offset", nullable = false)
    private Byte t01GmtOffset;

    @NotNull
    @Column(name = "T01_capacidad", nullable = false)
    private Short t01Capacidad;

    @Size(max = 24)
    @Column(name = "T01_lat_dms", length = 24)
    private String t01LatDms;

    @Size(max = 24)
    @Column(name = "T01_lon_dms", length = 24)
    private String t01LonDms;

    @NotNull
    @Column(name = "T01_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal t01Lat;

    @NotNull
    @Column(name = "T01_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal t01Lon;

}