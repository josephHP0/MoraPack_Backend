package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t05_cliente", schema = "morapack2")
public class T05Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T05_idCliente", nullable = false)
    private Integer id;

    @Size(max = 120)
    @Column(name = "T05_nombre", length = 120)
    private String t05Nombre;

    @Size(max = 160)
    @Column(name = "T05_contacto", length = 160)
    private String t05Contacto;

}