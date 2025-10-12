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
@Table(name = "t01_aeropuerto", schema = "morapack2", indexes = {
        @Index(name = "idx_aeropuerto_ciudad", columnList = "T08_idCiudad")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_aeropuerto_icao", columnNames = {"T01_codigoICAO"})
})
public class T01Aeropuerto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T01_idAeropuerto", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T08_idCiudad", nullable = false)
    private T08Ciudad t08Idciudad;

    @Size(max = 4)
    @NotNull
    @Column(name = "T01_codigoICAO", nullable = false, length = 4)
    private String t01Codigoicao;

    @Column(name = "T01_lat", precision = 9, scale = 6)
    private BigDecimal t01Lat;

    @Column(name = "T01_lon", precision = 9, scale = 6)
    private BigDecimal t01Lon;

    @Column(name = "T01_gmt_offset")
    private Short t01GmtOffset;

    @Column(name = "T01_capacidad")
    private Integer t01Capacidad;

    @Size(max = 40)
    @Column(name = "T01_alias", length = 40)
    private String t01Alias;

}