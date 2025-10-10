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
@Table(name = "t07_evento_tracking", schema = "morapack")
public class T07EventoTracking {
    @Id
    @Size(max = 32)
    @Column(name = "T07_idEvento", nullable = false, length = 32)
    private String t07Idevento;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "T07_idPaquete", nullable = false)
    private T04Paquete t07Idpaquete;

    @NotNull
    @Column(name = "T07_timestamp", nullable = false)
    private Instant t07Timestamp;

    @NotNull
    @Lob
    @Column(name = "T07_estadoOficial", nullable = false)
    private String t07Estadooficial;

    @Size(max = 255)
    @Column(name = "T07_nota")
    private String t07Nota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T07_idAeropuerto")
    private T01Aeropuerto t07Idaeropuerto;

}