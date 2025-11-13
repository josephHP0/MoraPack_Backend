package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class HeuristicCalculator {

    private final AcoParameters params;

    /**
     * Calcula el valor heurístico para asignar un pedido a un vuelo.
     * Valores más altos = mejor elección
     */
    public double calcularHeuristica(T02Pedido pedido, T04VueloProgramado vuelo, Instant tiempoActual) {

        // Factor SLA: cuánto tiempo queda antes de vencer el SLA
        double factorSla = calcularFactorSla(pedido, vuelo);

        // Factor capacidad: cuánto espacio disponible tiene el vuelo
        double factorCapacidad = calcularFactorCapacidad(vuelo, pedido.getT02Cantidad());

        // Factor tiempo: qué tan pronto sale el vuelo
        double factorTiempo = calcularFactorTiempo(vuelo, tiempoActual);

        // Combinación ponderada
        return (params.getPesoSla() * factorSla) +
               (params.getPesoCapacidad() * factorCapacidad) +
               (params.getPesoTiempo() * factorTiempo);
    }

    private double calcularFactorSla(T02Pedido pedido, T04VueloProgramado vuelo) {
        // Simplificado: asumimos que vuelos más tempranos ayudan a cumplir SLA
        // En implementación real, calcular tiempo restante hasta deadline
        return 1.0;
    }

    private double calcularFactorCapacidad(T04VueloProgramado vuelo, Integer cantidadProductos) {
        int capacidadDisponible = vuelo.getT04CapacidadTotal() - vuelo.getT04OcupacionTotal();
        if (capacidadDisponible <= 0) return 0.0;

        // Normalizar entre 0 y 1
        return Math.min(1.0, (double) capacidadDisponible / vuelo.getT04CapacidadTotal());
    }

    private double calcularFactorTiempo(T04VueloProgramado vuelo, Instant tiempoActual) {
        long horasHastaSalida = Duration.between(tiempoActual, vuelo.getT04FechaSalida()).toHours();

        // Penalizar vuelos muy lejanos o que ya salieron
        if (horasHastaSalida < 0) return 0.0;
        if (horasHastaSalida > 168) return 0.1; // Más de una semana

        // Preferir vuelos que salen pronto pero no inmediatamente (necesitamos 1h mínima de estancia)
        return 1.0 / (1.0 + horasHastaSalida / 24.0);
    }
}
