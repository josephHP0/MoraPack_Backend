package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Entidad que representa una cancelación de vuelo.
 * Registra cancelaciones manuales, por archivo o automáticas del sistema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "t12_cancelacion", schema = "morapack2", indexes = {
        @Index(name = "idx_canc_vuelo", columnList = "T06_idTramoVuelo"),
        @Index(name = "idx_canc_fecha", columnList = "T12_fechaCancelacion")
})
public class T12Cancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T12_idCancelacion", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T06_idTramoVuelo", nullable = false)
    private T06VueloProgramado vueloProgramado;

    @NotNull
    @Column(name = "T12_fechaCancelacion", nullable = false)
    private Instant fechaCancelacion;

    @Size(max = 200)
    @Column(name = "T12_motivo", length = 200)
    private String motivo;

    @Size(max = 20)
    @ColumnDefault("'MANUAL'")
    @Column(name = "T12_origen", length = 20)
    private String origen; // MANUAL, ARCHIVO, SISTEMA

    @PrePersist
    protected void onCreate() {
        if (fechaCancelacion == null) {
            fechaCancelacion = Instant.now();
        }
        if (origen == null) {
            origen = "MANUAL";
        }
    }
}
