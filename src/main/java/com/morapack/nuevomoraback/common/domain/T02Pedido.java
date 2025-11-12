package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t02_pedido", schema = "morapack2")
public class T02Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T02_ID", nullable = false)
    private Integer id;

    @Size(max = 45)
    @Column(name = "T02_ID_CADENA", length = 45)
    private String t02IdCadena;

    @Column(name = "T02_FECHA_PEDIDO")
    private Instant t02FechaPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T02_ID_AEROP_DESTINO")
    private T01Aeropuerto t02IdAeropDestino;

    @Column(name = "T02_CANTIDAD")
    private Integer t02Cantidad;

    @Column(name = "T02_ID_CLIENTE")
    private Integer t02IdCliente;

}