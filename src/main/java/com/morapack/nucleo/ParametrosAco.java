package com.morapack.nucleo;

import lombok.*;

import java.time.Duration;

/**
 * Parámetros configurables del algoritmo ACO.
 * Encapsula todos los pesos, umbrales y configuraciones del algoritmo de planificación.
 *
 * Basado en el proyecto de referencia dp16F_morapack-Lhia_simulacion,
 * adaptado para integración con la BD de MoraPack.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametrosAco {

    // ========== Parámetros ACO Básicos ==========

    /** Peso de la feromona en la probabilidad de transición (típicamente 0.5-1.0) */
    @Builder.Default
    private double alpha = 0.8;

    /** Peso de la heurística en la probabilidad de transición (típicamente 2.0-5.0) */
    @Builder.Default
    private double beta = 3.5;

    /** Tasa de evaporación de feromonas (0-1) */
    @Builder.Default
    private double rho = 0.35;

    /** Valor inicial de feromona en todos los arcos */
    @Builder.Default
    private double pheromoneInit = 1.0;

    /** Piso mínimo de feromona (evita lockout completo) */
    @Builder.Default
    private double pheromoneFloor = 1e-6;

    /** Tamaño de lista candidata (top-K por heurística) */
    @Builder.Default
    private int candidateK = 3;

    /** Máximo número de saltos permitidos en una ruta */
    @Builder.Default
    private int maxHops = 3;

    /** Número de hormigas por iteración */
    @Builder.Default
    private int numHormigas = 3;

    /** Número de iteraciones ACO por pedido */
    @Builder.Default
    private int numIteraciones = 1;

    // ========== Pesos de Heurística Multi-objetivo ==========

    /** Peso del tiempo de llegada (ETA) */
    @Builder.Default
    private double wTime = 1.0;

    /** Peso de la espera estimada en conexiones */
    @Builder.Default
    private double wWait = 1.0;

    /** Peso del riesgo de violar SLA */
    @Builder.Default
    private double wSla = 2.0;

    /** Peso de la congestión en hubs */
    @Builder.Default
    private double wCong = 3.0;

    /** Penalización por salto adicional */
    @Builder.Default
    private double wHops = 0.3;

    // ========== Parámetros de Congestión ==========

    /** Umbral de ocupación para penalización de congestión (0-1) */
    @Builder.Default
    private double congestionThreshold = 0.60;

    /** Curvatura de la penalización cuadrática de congestión */
    @Builder.Default
    private double congestionGamma = 8.0;

    // ========== Parámetros Operacionales ==========

    /** Horizonte temporal para validar capacidad de salida futura */
    @Builder.Default
    private Duration headroomHorizon = Duration.ofHours(6);

    /** Tiempo mínimo de permanencia en almacén (dwell time) */
    @Builder.Default
    private Duration dwellMin = Duration.ofHours(1);

    /** Ratio de capacidad reservada para tránsito (0-1) */
    @Builder.Default
    private double reserveTransitRatio = 0.15;

    /** Referencia para normalización de tiempo de llegada */
    @Builder.Default
    private Duration etaRef = Duration.ofHours(24);

    /** Referencia para normalización de tiempo de espera */
    @Builder.Default
    private Duration waitRef = Duration.ofHours(6);

    /** SLA intra-continental en horas */
    @Builder.Default
    private double slaIntraContinental = 48.0; // 2 días

    /** SLA inter-continental en horas */
    @Builder.Default
    private double slaInterContinental = 72.0; // 3 días

    // ========== Métodos de Utilidad ==========

    /**
     * Calcula la penalización de congestión basada en la ocupación.
     * Penalización cuadrática con umbral.
     *
     * @param u Ocupación relativa (0-1)
     * @return Penalización (0 si u <= threshold, cuadrática si u > threshold)
     */
    public double congestionPenalty(double u) {
        if (u <= congestionThreshold) {
            return 0.0;
        }
        double excess = u - congestionThreshold;
        return congestionGamma * excess * excess;
    }

    /**
     * Crea una instancia con valores por defecto.
     *
     * @return ParametrosAco con configuración por defecto
     */
    public static ParametrosAco defaults() {
        return ParametrosAco.builder().build();
    }

    /**
     * Valida que los parámetros están en rangos aceptables.
     *
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    public void validate() {
        if (alpha < 0 || alpha > 10) {
            throw new IllegalArgumentException("alpha debe estar en [0, 10]");
        }
        if (beta < 0 || beta > 10) {
            throw new IllegalArgumentException("beta debe estar en [0, 10]");
        }
        if (rho < 0 || rho > 1) {
            throw new IllegalArgumentException("rho debe estar en [0, 1]");
        }
        if (pheromoneInit <= 0) {
            throw new IllegalArgumentException("pheromoneInit debe ser positivo");
        }
        if (pheromoneFloor <= 0 || pheromoneFloor >= pheromoneInit) {
            throw new IllegalArgumentException("pheromoneFloor debe estar en (0, pheromoneInit)");
        }
        if (candidateK < 1) {
            throw new IllegalArgumentException("candidateK debe ser al menos 1");
        }
        if (maxHops < 1) {
            throw new IllegalArgumentException("maxHops debe ser al menos 1");
        }
        if (numHormigas < 1) {
            throw new IllegalArgumentException("numHormigas debe ser al menos 1");
        }
        if (numIteraciones < 1) {
            throw new IllegalArgumentException("numIteraciones debe ser al menos 1");
        }
        if (congestionThreshold < 0 || congestionThreshold > 1) {
            throw new IllegalArgumentException("congestionThreshold debe estar en [0, 1]");
        }
        if (reserveTransitRatio < 0 || reserveTransitRatio > 1) {
            throw new IllegalArgumentException("reserveTransitRatio debe estar en [0, 1]");
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ParametrosAco{alpha=%.2f, beta=%.2f, rho=%.2f, " +
            "hormigas=%d, iteraciones=%d, maxHops=%d, " +
            "congThreshold=%.2f, headroom=%dh}",
            alpha, beta, rho, numHormigas, numIteraciones, maxHops,
            congestionThreshold, headroomHorizon.toHours()
        );
    }
}
