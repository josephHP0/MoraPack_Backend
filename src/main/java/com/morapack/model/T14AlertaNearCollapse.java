package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad que representa una alerta de near-collapse en un aeropuerto.
 * Registra situaciones donde un aeropuerto está cerca de su capacidad máxima.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "t14_alerta_near_collapse", schema = "morapack2", indexes = {
        @Index(name = "idx_alert_sim", columnList = "T10_idSimulacion"),
        @Index(name = "idx_alert_aerop", columnList = "T01_idAeropuerto"),
        @Index(name = "idx_alert_fecha", columnList = "T14_fechaHora")
})
public class T14AlertaNearCollapse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T14_idAlerta", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T10_idSimulacion", nullable = false)
    private T10SimulacionSemanal simulacionSemanal;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T01_idAeropuerto", nullable = false)
    private T01Aeropuerto aeropuerto;

    @NotNull
    @Column(name = "T14_fechaHora", nullable = false)
    private Instant fechaHora;

    @NotNull
    @Column(name = "T14_wps", nullable = false, precision = 5, scale = 2)
    private BigDecimal wps;

    @NotNull
    @Column(name = "T14_ocupacion", nullable = false)
    private Integer ocupacion;

    @NotNull
    @Column(name = "T14_capacidad", nullable = false)
    private Integer capacidad;

    @Size(max = 20)
    @ColumnDefault("'MEDIA'")
    @Column(name = "T14_severidad", length = 20)
    private String severidad; // BAJA, MEDIA, ALTA, CRITICA

    @ColumnDefault("false")
    @Column(name = "T14_resuelto")
    private Boolean resuelto;

    @PrePersist
    protected void onCreate() {
        if (severidad == null) {
            severidad = calcularSeveridad();
        }
        if (resuelto == null) {
            resuelto = false;
        }
    }

    /**
     * Calcula la severidad basada en WPS y ocupación
     */
    private String calcularSeveridad() {
        double wpsValue = wps != null ? wps.doubleValue() : 0.0;
        double ocupacionPct = capacidad > 0 ? (ocupacion * 100.0 / capacidad) : 0.0;

        if (wpsValue >= 1.2 || ocupacionPct >= 95) {
            return "CRITICA";
        } else if (wpsValue >= 0.9 || ocupacionPct >= 85) {
            return "ALTA";
        } else if (wpsValue >= 0.7 || ocupacionPct >= 75) {
            return "MEDIA";
        } else {
            return "BAJA";
        }
    }
}
