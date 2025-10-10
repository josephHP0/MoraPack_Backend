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
@Table(name = "t11_avion", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_t11_matricula", columnNames = {"T11_matricula"})
})
public class T11Avion {
    @Id
    @Size(max = 32)
    @Column(name = "T11_idAvion", nullable = false, length = 32)
    private String t11Idavion;

    @Size(max = 16)
    @NotNull
    @Column(name = "T11_matricula", nullable = false, length = 16)
    private String t11Matricula;

    @Size(max = 40)
    @NotNull
    @Column(name = "T11_modelo", nullable = false, length = 40)
    private String t11Modelo;

    @NotNull
    @Column(name = "T11_capacidadMax", nullable = false)
    private Short t11Capacidadmax;

    @Size(max = 40)
    @NotNull
    @Column(name = "T11_operador", nullable = false, length = 40)
    private String t11Operador;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "T11_activo", nullable = false)
    private Boolean t11Activo = false;

}