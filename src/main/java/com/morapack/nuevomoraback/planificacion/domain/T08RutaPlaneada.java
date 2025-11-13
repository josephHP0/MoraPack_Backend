package com.morapack.nuevomoraback.planificacion.domain;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t08_ruta_planeada", schema = "morapack2")
public class T08RutaPlaneada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T08_ID_RUTA_PLANEADA", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T02_ID_PEDIDO", nullable = false)
    private T02Pedido pedido;

    @Column(name = "T08_FECHA_PLANIFICACION", nullable = false)
    private Instant fechaPlanificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "T08_TIPO_SIMULACION", nullable = false)
    private TipoSimulacion tipoSimulacion;

    @Column(name = "T08_FECHA_ENTREGA_ESTIMADA")
    private Instant fechaEntregaEstimada;

    @ColumnDefault("false")
    @Column(name = "T08_CUMPLE_SLA")
    private Boolean cumpleSla = false;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDIENTE'")
    @Column(name = "T08_ESTADO")
    private EstadoRuta estado = EstadoRuta.PENDIENTE;

    @OneToMany(mappedBy = "rutaPlaneada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<T09TramoAsignado> tramosAsignados = new ArrayList<>();

    public enum TipoSimulacion {
        SEMANAL, DIA_A_DIA, COLAPSO
    }

    public enum EstadoRuta {
        PENDIENTE, EN_TRANSITO, ENTREGADO, RECHAZADO
    }
}
