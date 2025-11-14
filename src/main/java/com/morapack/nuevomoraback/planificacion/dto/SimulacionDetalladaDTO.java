package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO completo de una simulación semanal para el frontend.
 * Incluye toda la información necesaria para visualizar:
 * - Vuelos con su ocupación
 * - Pedidos con sus rutas completas
 * - Métricas de la simulación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionDetalladaDTO {

    // Metadata de la simulación
    private Integer idResultado;
    private String tipoSimulacion;
    private Instant fechaInicio;
    private Instant fechaFin;
    private Instant fechaEjecucion;
    private Long duracionMs;
    private String estado;
    private String mensaje;

    // Configuración
    private Integer duracionBloqueHoras; // null si no se usaron bloques
    private Integer numeroBloquesEjecutados;

    // Métricas generales
    private MetricasDTO metricas;

    // Datos detallados para visualización
    private List<VueloDetalladoDTO> vuelos;
    private List<PedidoRutaDTO> pedidos;

    // Estadísticas adicionales
    private EstadisticasDetalladasDTO estadisticas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasDetalladasDTO {
        // Vuelos
        private Integer totalVuelosDisponibles;
        private Integer vuelosUtilizados;
        private Integer vuelosNoUtilizados;
        private Double porcentajeUtilizacionVuelos;

        // Ocupación
        private Double ocupacionPromedioTodosVuelos;
        private Double ocupacionPromedioVuelosUtilizados;
        private Integer vuelosConSobrecarga;

        // Pedidos por estado
        private Integer pedidosPendientes;
        private Integer pedidosEnTransito;
        private Integer pedidosEntregados;
        private Integer pedidosRechazados;

        // SLA
        private Integer pedidosCumplenSla;
        private Integer pedidosNoCumplenSla;
        private Double porcentajeCumplimientoSla;
    }
}
