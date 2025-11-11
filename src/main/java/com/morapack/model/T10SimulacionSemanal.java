package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Entidad que representa una simulación semanal de planificación logística.
 * Registra el estado, progreso y resultados de cada ejecución de simulación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "t10_simulacion_semanal", schema = "morapack2", indexes = {
        @Index(name = "idx_sim_estado", columnList = "T10_estado"),
        @Index(name = "idx_sim_fecha", columnList = "T10_fechaCreacion")
})
public class T10SimulacionSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T10_idSimulacion", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "T10_fechaInicio", nullable = false)
    private Instant fechaInicio;

    @NotNull
    @Column(name = "T10_fechaFin", nullable = false)
    private Instant fechaFin;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "T10_fechaCreacion")
    private Instant fechaCreacion;

    @NotNull
    @Column(name = "T10_estado", nullable = false, length = 30)
    @ColumnDefault("'EN_PROGRESO'")
    private String estado; // EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA

    @ColumnDefault("0")
    @Column(name = "T10_pedidosProcesados")
    private Integer pedidosProcesados;

    @ColumnDefault("0")
    @Column(name = "T10_pedidosAsignados")
    private Integer pedidosAsignados;

    @ColumnDefault("0")
    @Column(name = "T10_pedidosPendientes")
    private Integer pedidosPendientes;

    @Column(name = "T10_motivoFallo", length = 500)
    private String motivoFallo;

    @Column(name = "T10_duracionMs")
    private Long duracionMs;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
        if (estado == null) {
            estado = "EN_PROGRESO";
        }
        if (pedidosProcesados == null) pedidosProcesados = 0;
        if (pedidosAsignados == null) pedidosAsignados = 0;
        if (pedidosPendientes == null) pedidosPendientes = 0;
    }
}
