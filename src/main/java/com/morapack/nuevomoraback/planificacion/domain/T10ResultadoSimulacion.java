package com.morapack.nuevomoraback.planificacion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "t10_resultado_simulacion", schema = "morapack2")
public class T10ResultadoSimulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T10_ID_RESULTADO", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "T10_TIPO_SIMULACION", nullable = false)
    private T08RutaPlaneada.TipoSimulacion tipoSimulacion;

    @Column(name = "T10_FECHA_INICIO", nullable = false)
    private Instant fechaInicio;

    @Column(name = "T10_FECHA_FIN", nullable = false)
    private Instant fechaFin;

    @Column(name = "T10_FECHA_EJECUCION", nullable = false)
    private Instant fechaEjecucion;

    @Column(name = "T10_DURACION_MS")
    private Long duracionMs;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EN_PROGRESO'")
    @Column(name = "T10_ESTADO")
    private EstadoSimulacion estado = EstadoSimulacion.EN_PROGRESO;

    @Lob
    @Column(name = "T10_MENSAJE")
    private String mensaje;

    @OneToOne(mappedBy = "resultadoSimulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private T11MetricasSimulacion metricas;

    public enum EstadoSimulacion {
        EN_PROGRESO, COMPLETADO, ERROR
    }
}
