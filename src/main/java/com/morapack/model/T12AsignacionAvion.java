package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t12_asignacion_avion", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_t12_tramo", columnNames = {"T12_idTramo"})
})
public class T12AsignacionAvion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T12_idAsignAvion", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "T12_idTramo", nullable = false)
    private T06VueloProgramado t12Idtramo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T12_idAvion", nullable = false)
    private T11Avion t12Idavion;

    @NotNull
    @ColumnDefault("'ASIGNADO'")
    @Lob
    @Column(name = "T12_estado", nullable = false)
    private String t12Estado;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "T12_fechaAsignacion", nullable = false)
    private Instant t12Fechaasignacion;

}