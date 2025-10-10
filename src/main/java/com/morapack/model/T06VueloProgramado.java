package com.morapack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t06_vuelo_programado", schema = "morapack", uniqueConstraints = {
        @UniqueConstraint(name = "uq_t06_ruta_fecha", columnNames = {"T06_idAeropuertoOrigen", "T06_idAeropuertoDestino", "T06_fechaSalida"})
})
public class T06VueloProgramado {
    @Id
    @Size(max = 32)
    @Column(name = "T06_idTramo", nullable = false, length = 32)
    private String t06Idtramo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T06_idAeropuertoOrigen", nullable = false)
    private T01Aeropuerto t06Idaeropuertoorigen;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T06_idAeropuertoDestino", nullable = false)
    private T01Aeropuerto t06Idaeropuertodestino;

    @NotNull
    @Column(name = "T06_fechaSalida", nullable = false)
    private Instant t06Fechasalida;

    @NotNull
    @Column(name = "T06_fechaLlegada", nullable = false)
    private Instant t06Fechallegada;

    @NotNull
    @Lob
    @Column(name = "T06_tipo", nullable = false)
    private String t06Tipo;

    @NotNull
    @Column(name = "T06_capacidadMax", nullable = false)
    private Integer t06Capacidadmax;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "T06_ocupacionActual", nullable = false)
    private Integer t06Ocupacionactual;

    @Size(max = 40)
    @NotNull
    @ColumnDefault("'PACK'")
    @Column(name = "T06_operador", nullable = false, length = 40)
    private String t06Operador;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "T06_cancelado", nullable = false)
    private Boolean t06Cancelado = false;

}