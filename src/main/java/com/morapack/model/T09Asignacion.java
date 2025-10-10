package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t09_asignacion", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_asig_paquete_tramo_orden", columnNames = {"T09_idPaquete", "T09_idTramo", "T09_orden"})
})
public class T09Asignacion {
    @Id
    @Size(max = 32)
    @Column(name = "T09_idAsig", nullable = false, length = 32)
    private String t09Idasig;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "T09_idPaquete", nullable = false)
    private T04Paquete t09Idpaquete;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T09_idTramo", nullable = false)
    private T06VueloProgramado t09Idtramo;

    @NotNull
    @Column(name = "T09_orden", nullable = false)
    private Integer t09Orden;

    @Column(name = "T09_fechaCompromiso")
    private Instant t09Fechacompromiso;

    @NotNull
    @Lob
    @Column(name = "T09_estadoAsignacion", nullable = false)
    private String t09Estadoasignacion;

}