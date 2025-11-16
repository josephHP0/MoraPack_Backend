package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.planificacion.dto.*;

import java.time.Instant;

public interface PlanificadorSemanalService {

    /**
     * Ejecuta simulación semanal batch (con o sin bloques)
     * Si duracionBloqueHoras es null, ejecuta todo de golpe
     * Si tiene valor, divide la simulación en bloques del tamaño especificado
     */
    SimulacionDetalladaDTO ejecutarSimulacionSemanal(SimulacionSemanalRequest request);

    /**
     * Consulta resultado de simulación por ID con información detallada
     */
    SimulacionDetalladaDTO consultarResultado(Integer idResultado);

    /**
     * Debug - Verifica cuántos pedidos hay disponibles en un rango de fechas
     */
    DebugPedidosDTO debugPedidosDisponibles(Instant fechaInicio, Instant fechaFin);

    /**
     * NUEVO - Procesa y devuelve UN SOLO BLOQUE de simulación.
     * Diseñado para streaming incremental desde el front.
     *
     * El front puede solicitar bloques secuencialmente a medida que avanza la visualización:
     * 1. Bloque 1: 00:00 - 10:00 (front lo muestra)
     * 2. Bloque 2: 10:00 - 20:00 (cuando front está cerca de terminar bloque 1)
     * 3. Y así sucesivamente...
     *
     * @param request Parámetros del bloque (rango temporal)
     * @return Datos del bloque procesado (vuelos, pedidos, rutas del rango)
     */
    BloqueSimulacionResponse procesarBloqueIncremental(BloqueSimulacionRequest request);
}
