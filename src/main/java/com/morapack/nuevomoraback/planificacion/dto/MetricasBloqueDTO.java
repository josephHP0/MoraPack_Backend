package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métricas parciales de un bloque de simulación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasBloqueDTO {

    /**
     * Total de pedidos procesados en este bloque
     */
    private Integer totalPedidosProcesados;

    /**
     * Pedidos asignados exitosamente
     */
    private Integer pedidosAsignados;

    /**
     * Pedidos no asignados (sin vuelos disponibles)
     */
    private Integer pedidosNoAsignados;

    /**
     * Total de vuelos utilizados en este bloque
     */
    private Integer vuelosUtilizados;

    /**
     * Capacidad total ocupada
     */
    private Integer capacidadOcupada;

    /**
     * Tiempo de procesamiento de este bloque (ms)
     */
    private Long tiempoProcesamiento;
}
