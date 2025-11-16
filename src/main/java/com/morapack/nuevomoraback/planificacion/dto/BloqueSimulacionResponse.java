package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta de un bloque individual de simulación.
 * Contiene solo los datos del rango temporal solicitado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueSimulacionResponse {

    /**
     * Identificador del resultado de simulación (para tracking en el front)
     */
    private Integer idResultadoSimulacion;

    /**
     * Inicio del bloque procesado
     */
    private Instant fechaInicio;

    /**
     * Fin del bloque procesado
     */
    private Instant fechaFin;

    /**
     * Número del bloque (1-indexed)
     */
    private Integer numeroBloque;

    /**
     * Vuelos en este bloque (solo los que operan en el rango temporal)
     */
    private List<VueloSimuladoDTO> vuelos;

    /**
     * Pedidos planificados en este bloque
     */
    private List<PedidoDetalleDTO> pedidos;

    /**
     * Rutas completas que incluyen vuelos en este bloque
     */
    private List<RutaCompletaDTO> rutas;

    /**
     * Métricas parciales de este bloque
     */
    private MetricasBloqueDTO metricas;

    /**
     * Indica si hay más bloques disponibles
     */
    private Boolean hayMasBloques;

    /**
     * Sugerencia de cuándo el front debería solicitar el siguiente bloque
     * (timestamp en el futuro de la simulación)
     */
    private Instant sugerenciaSolicitudSiguienteBloque;
}
