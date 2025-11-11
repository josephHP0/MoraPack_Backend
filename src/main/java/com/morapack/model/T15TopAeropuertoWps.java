package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa el ranking de aeropuertos con mayor WPS en un día.
 * Registra el top-5 de aeropuertos más congestionados para métricas diarias.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "t15_top_aeropuerto_wps", schema = "morapack2", indexes = {
        @Index(name = "idx_top_metrica", columnList = "T13_idMetrica")
})
public class T15TopAeropuertoWps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T15_idTop", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T13_idMetrica", nullable = false)
    private T13MetricaDiaria metricaDiaria;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuerto", nullable = false)
    private T01Aeropuerto aeropuerto;

    @NotNull
    @Column(name = "T15_wpsMaximo", nullable = false, precision = 5, scale = 2)
    private BigDecimal wpsMaximo;

    @NotNull
    @Column(name = "T15_ranking", nullable = false)
    private Integer ranking; // 1 a 5
}
