package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t03_cliente", schema = "morapack2")
public class T03Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T03_ID_CLIENTE", nullable = false)
    private Integer id;

    @Size(max = 120)
    @Column(name = "T03_NOMBRE", length = 120)
    private String t03Nombre;

    @Size(max = 160)
    @Column(name = "T03_CONTACTO", length = 160)
    private String t03Contacto;

}