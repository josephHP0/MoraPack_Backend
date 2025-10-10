package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "t10_plan_vuelo", schema = "morapack")
public class T10PlanVuelo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T10_idPlan", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T10_idAeropuertoOrigen", nullable = false)
    private T01Aeropuerto t10Idaeropuertoorigen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T10_idAeropuertoDestino", nullable = false)
    private T01Aeropuerto t10Idaeropuertodestino;

    @NotNull
    @Column(name = "T10_horaSalida", nullable = false)
    private LocalTime t10Horasalida;

    @NotNull
    @Column(name = "T10_horaLlegada", nullable = false)
    private LocalTime t10Horallegada;

    @Column(name = "T10_capacidadBase")
    private Short t10Capacidadbase;

    @Size(max = 40)
    @Column(name = "T10_operador", length = 40)
    private String t10Operador;

    @NotNull
    @ColumnDefault("'MON,TUE,WED,THU,FRI,SAT,SUN'")
    @Lob
    @Column(name = "T10_dow", nullable = false)
    private String t10Dow;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "T10_activo", nullable = false)
    private Boolean t10Activo = false;

}