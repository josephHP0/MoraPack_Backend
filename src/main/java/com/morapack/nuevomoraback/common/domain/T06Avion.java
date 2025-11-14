package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t06_avion", schema = "morapack2")
public class T06Avion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T06_ID_AVION", nullable = false)
    private Integer id;

    @Size(max = 16)
    @NotNull
    @Column(name = "T06_MATRICULA", nullable = false, length = 16)
    private String t06Matricula;

    @Size(max = 40)
    @Column(name = "T06_MODELO", length = 40)
    private String t06Modelo;

    @NotNull
    @Column(name = "T06_CAPACIDAD_MAXIMA", nullable = false)
    private Integer t06CapacidadMaxima;

    @Size(max = 40)
    @Column(name = "T06_OPERADOR", length = 40)
    private String t06Operador;

    @Lob
    @Column(name = "T06_ACTIVO")
    private String t06Activo;

}