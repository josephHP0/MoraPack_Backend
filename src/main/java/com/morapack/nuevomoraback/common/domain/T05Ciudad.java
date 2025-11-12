package com.morapack.nuevomoraback.common.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "t05_ciudad", schema = "morapack2")
public class T05Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T05_ID_CIUDAD", nullable = false)
    private Integer id;

    @Size(max = 80)
    @NotNull
    @Column(name = "T05_NOMBRE", nullable = false, length = 80)
    private String t05Nombre;

    @Size(max = 40)
    @Column(name = "T05_CONTINENTE", length = 40)
    private String t05Continente;

    @Size(max = 64)
    @Column(name = "T05_ZONA_HORARIA", length = 64)
    private String t05ZonaHoraria;

    @ColumnDefault("0")
    @Column(name = "T05_ES_HUB")
    private Boolean t05EsHub;

}