package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t04_paquete", schema = "morapack")
public class T04Paquete {
    @Id
    @Size(max = 32)
    @Column(name = "T04_idPaquete", nullable = false, length = 32)
    private String t04Idpaquete;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T04_idPedido", nullable = false)
    private T03Pedido t04Idpedido;

    @NotNull
    @Lob
    @Column(name = "T04_estado", nullable = false)
    private String t04Estado;

    @Column(name = "T04_eta")
    private Instant t04Eta;

    @Column(name = "T04_tDisponible")
    private Instant t04Tdisponible;

    @Column(name = "T04_tRecibido")
    private Instant t04Trecibido;

}