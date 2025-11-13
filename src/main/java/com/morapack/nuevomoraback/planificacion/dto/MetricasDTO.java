package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricasDTO {
    private Integer totalPedidos;
    private Integer pedidosEntregados;
    private Integer pedidosEnTransito;
    private Integer pedidosRechazados;
    private BigDecimal cumplimientoSla;
    private BigDecimal ocupacionPromedio;
    private Integer vuelosUtilizados;
    private Integer vuelosCancelados;
}
