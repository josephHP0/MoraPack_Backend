package com.morapack.nuevomoraback.planificacion.domain;

import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t12_cancelacion_vuelo", schema = "morapack2")
public class T12CancelacionVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T12_ID_CANCELACION", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T04_ID_TRAMO_VUELO", nullable = false)
    private T04VueloProgramado vueloProgramado;

    @Column(name = "T12_FECHA_CANCELACION", nullable = false)
    private Instant fechaCancelacion;

    @Size(max = 255)
    @Column(name = "T12_MOTIVO", length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "T12_TIPO_SIMULACION")
    private T08RutaPlaneada.TipoSimulacion tipoSimulacion;
}
