package com.morapack.nuevomoraback.planificacion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t11_metricas_simulacion", schema = "morapack2")
public class T11MetricasSimulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "T11_ID_METRICA", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "T10_ID_RESULTADO", nullable = false)
    private T10ResultadoSimulacion resultadoSimulacion;

    @ColumnDefault("0")
    @Column(name = "T11_TOTAL_PEDIDOS")
    private Integer totalPedidos = 0;

    @ColumnDefault("0")
    @Column(name = "T11_PEDIDOS_ENTREGADOS")
    private Integer pedidosEntregados = 0;

    @ColumnDefault("0")
    @Column(name = "T11_PEDIDOS_EN_TRANSITO")
    private Integer pedidosEnTransito = 0;

    @ColumnDefault("0")
    @Column(name = "T11_PEDIDOS_RECHAZADOS")
    private Integer pedidosRechazados = 0;

    @Column(name = "T11_CUMPLIMIENTO_SLA", precision = 5, scale = 2)
    private BigDecimal cumplimientoSla;

    @Column(name = "T11_OCUPACION_PROMEDIO", precision = 5, scale = 2)
    private BigDecimal ocupacionPromedio;

    @ColumnDefault("0")
    @Column(name = "T11_VUELOS_UTILIZADOS")
    private Integer vuelosUtilizados = 0;

    @ColumnDefault("0")
    @Column(name = "T11_VUELOS_CANCELADOS")
    private Integer vuelosCancelados = 0;
}
