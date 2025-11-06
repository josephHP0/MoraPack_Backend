package com.morapack.model.NuevoPedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import com.morapack.model.T01Aeropuerto;

@Getter
@Setter
@Entity
@Table(name = "t03_pedido", schema = "morapack2", indexes = {
        @Index(name = "T03_ID_AEROP_DESTINO_FK_idx", columnList = "T03_ID_AEROP_DESTINO")
})
public class T03Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T03_ID", nullable = false)
    private Integer id;

    @Size(max = 45)
    @Column(name = "T03_ID_CADENA", length = 45)
    private String t03IdCadena;

    @Column(name = "T03_FECHA_PEDIDO")
    private Instant t03FechaPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "T03_ID_AEROP_DESTINO")
    private T01Aeropuerto t03IdAeropDestino;

    @Column(name = "T03_CANTIDAD")
    private Integer t03Cantidad;

    @Column(name = "T03_ID_CLIENTE")
    private Integer t03IdCliente;
}