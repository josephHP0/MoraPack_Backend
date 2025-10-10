package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t05_cliente", schema = "morapack", indexes = {
        @Index(name = "idx_cliente_nombre", columnList = "T05_nombre")
})
public class T05Cliente {
    @Id
    @Size(max = 32)
    @Column(name = "T05_idCliente", nullable = false, length = 32)
    private String t05Idcliente;

    @Size(max = 120)
    @NotNull
    @Column(name = "T05_nombre", nullable = false, length = 120)
    private String t05Nombre;

    @Size(max = 160)
    @Column(name = "T05_contacto", length = 160)
    private String t05Contacto;

}