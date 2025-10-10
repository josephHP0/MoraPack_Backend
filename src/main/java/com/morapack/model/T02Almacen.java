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
@Table(name = "t02_almacen", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_t02_idAeropuerto", columnNames = {"T02_idAeropuerto"})
})
public class T02Almacen {
    @Id
    @Size(max = 32)
    @Column(name = "T02_idAlmacen", nullable = false, length = 32)
    private String t02Idalmacen;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T02_idAeropuerto", nullable = false)
    private T01Aeropuerto t02Idaeropuerto;

    @NotNull
    @Column(name = "T02_capacidadMax", nullable = false)
    private Integer t02Capacidadmax;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "T02_capacidadActual", nullable = false)
    private Integer t02Capacidadactual;

}