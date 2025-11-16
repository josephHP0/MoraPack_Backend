package com.morapack.nuevomoraback.planificacion.aco;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AcoParameters {

    // Parámetros del algoritmo ACO
    @Value("${aco.alpha:1.0}")
    private double alpha;           // Importancia de la feromona

    @Value("${aco.beta:2.0}")
    private double beta;            // Importancia de la heurística

    @Value("${aco.rho:0.5}")
    private double rho;             // Tasa de evaporación

    @Value("${aco.q0:0.9}")
    private double q0;              // Probabilidad de explotar vs explorar

    @Value("${aco.numero-hormigas:50}")
    private int numeroHormigas;

    @Value("${aco.numero-iteraciones:100}")
    private int numeroIteraciones;

    @Value("${aco.feromona-inicial:0.1}")
    private double feromonaInicial;

    // Pesos para la función heurística
    @Value("${aco.peso-sla:0.4}")
    private double pesoSla;

    @Value("${aco.peso-capacidad:0.3}")
    private double pesoCapacidad;

    @Value("${aco.peso-tiempo:0.3}")
    private double pesoTiempo;
}
