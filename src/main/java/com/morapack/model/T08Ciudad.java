package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "t08_ciudad", schema = "morapack2", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ciudad_nombre", columnNames = {"T08_nombre"})
})
public class T08Ciudad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T08_idCiudad", nullable = false)
    private Integer id;

    @Size(max = 80)
    @NotNull
    @Column(name = "T08_nombre", nullable = false, length = 80)
    private String t08Nombre;

    @Size(max = 40)
    @Column(name = "T08_continente", length = 40)
    private String t08Continente;

    @Size(max = 64)
    @Column(name = "T08_zonaHoraria", length = 64)
    private String t08Zonahoraria;

    @ColumnDefault("0")
    @Column(name = "T08_esHub")
    private Boolean t08Eshub;

}