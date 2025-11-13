package com.morapack.nuevomoraback.planificacion.domain;

import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "t09_tramo_asignado", schema = "morapack2")
public class T09TramoAsignado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T09_ID_TRAMO_ASIGNADO", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T08_ID_RUTA_PLANEADA", nullable = false)
    private T08RutaPlaneada rutaPlaneada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T04_ID_TRAMO_VUELO", nullable = false)
    private T04VueloProgramado vueloProgramado;

    @Column(name = "T09_CANTIDAD_PRODUCTOS", nullable = false)
    private Integer cantidadProductos;

    @Column(name = "T09_ORDEN_EN_RUTA", nullable = false)
    private Short ordenEnRuta;

    @ColumnDefault("false")
    @Column(name = "T09_ES_VUELO_FINAL")
    private Boolean esVueloFinal = false;
}
