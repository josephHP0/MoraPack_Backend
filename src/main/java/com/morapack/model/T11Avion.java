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
@Table(name = "t11_avion", schema = "morapack2", uniqueConstraints = {
        @UniqueConstraint(name = "uk_avion_matricula", columnNames = {"T11_matricula"})
})
public class T11Avion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T11_idAvion", nullable = false)
    private Integer id;

    @Size(max = 16)
    @NotNull
    @Column(name = "T11_matricula", nullable = false, length = 16)
    private String t11Matricula;

    @Size(max = 40)
    @Column(name = "T11_modelo", length = 40)
    private String t11Modelo;

    @NotNull
    @Column(name = "T11_capacidadMax", nullable = false)
    private Integer t11Capacidadmax;

    @Size(max = 40)
    @Column(name = "T11_operador", length = 40)
    private String t11Operador;

    @Column(name = "T11_activo", nullable = false, length = 30)
    private String t11Activo;

}