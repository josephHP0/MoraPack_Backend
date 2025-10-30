package com.morapack.nucleo;

import java.time.Duration;

/**
 * Parámetros de ACO y de la heurística "congestion-aware".
 * Ajustables sin recompilar el resto del núcleo.
 */
public class ParametrosAco {
    // ===== ACO Básico =====
    public double alpha = 0.8;        // Peso de feromona en la prob. de transición
    public double beta  = 3.5;        // Peso de heurística en la prob. de transición
    public double rho   = 0.35;       // Tasa de evaporación de feromona (0..1)

    public double pheromoneInit  = 1.0;    // Feromona inicial en arcos
    public double pheromoneFloor = 1e-6;   // Piso de feromona para evitar lockout

    public int candidateK = 3;  // Tamaño de la lista candidata (top-k por heurística)
    public int maxHops = 3;     // Saltos máximos permitidos en una ruta

    // ===== Heurística de costo =====
    public double w_time = 1.0;  // Peso del ETA
    public double w_wait = 1.0;  // Peso de la espera estimada
    public double w_sla  = 2.0;  // Peso del riesgo de violar SLA
    public double w_cong = 3.0;  // Peso de congestión
    public double w_hops = 0.3;  // Penalización por número de saltos

    // ===== Congestión =====
    public double congestionThreshold = 0.60;
    public double congestionGamma = 8.0;

    public double congestionPenalty(double u) {
        if (u <= congestionThreshold) return 0.0;
        double d = (u - congestionThreshold);
        return congestionGamma * d * d;
    }

    // ===== Headroom predictivo =====
    public Duration headroomHorizon = Duration.ofHours(6);
    public double headroomBlockCostMultiplier = 10.0;

    // ===== Operación =====
    public Duration dwellMin = Duration.ofHours(1);
    public double reserveTransitRatio = 0.15;

    // ===== Normalizaciones =====
    public Duration etaRef = Duration.ofHours(24);
    public Duration waitRef = Duration.ofHours(6);

    public static ParametrosAco defaults() {
        return new ParametrosAco();
    }

    // Builder methods
    public ParametrosAco withAlpha(double v){ this.alpha=v; return this; }
    public ParametrosAco withBeta(double v){ this.beta=v; return this; }
    public ParametrosAco withRho(double v){ this.rho=v; return this; }

    public ParametrosAco withWeights(double wTime, double wWait, double wSla, double wCong, double wHops){
        this.w_time=wTime; this.w_wait=wWait; this.w_sla=wSla; this.w_cong=wCong; this.w_hops=wHops;
        return this;
    }

    public ParametrosAco withCongestion(double threshold, double gamma){
        this.congestionThreshold=threshold; this.congestionGamma=gamma;
        return this;
    }

    public ParametrosAco withHeadroom(Duration horizon, double blockMultiplier){
        this.headroomHorizon=horizon; this.headroomBlockCostMultiplier=blockMultiplier;
        return this;
    }

    public ParametrosAco withCandidateK(int k){ this.candidateK=k; return this; }
    public ParametrosAco withMaxHops(int h){ this.maxHops=h; return this; }
    public ParametrosAco withReserves(double ratio){ this.reserveTransitRatio=ratio; return this; }

    public ParametrosAco withRefs(Duration etaRef, Duration waitRef){
        this.etaRef = etaRef; this.waitRef = waitRef;
        return this;
    }
}