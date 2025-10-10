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
@Table(name = "t08_ciudad", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ciudad_nombre", columnNames = {"T08_nombre"})
})
public class T08Ciudad {
    @Id
    @Size(max = 32)
    @Column(name = "T08_idCiudad", nullable = false, length = 32)
    private String t08Idciudad;

    @Size(max = 80)
    @NotNull
    @Column(name = "T08_nombre", nullable = false, length = 80)
    private String t08Nombre;

    @Size(max = 40)
    @NotNull
    @Column(name = "T08_continente", nullable = false, length = 40)
    private String t08Continente;

    @Size(max = 40)
    @NotNull
    @Column(name = "T08_zonaHoraria", nullable = false, length = 40)
    private String t08Zonahoraria;

    @ColumnDefault("0")
    @Column(name = "T08_esHub")
    private Boolean t08Eshub;

}