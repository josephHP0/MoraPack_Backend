package com.morapack.nuevomoraback.planificacion.aco;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class AcoParameters {

    // Parámetros del algoritmo ACO
    private double alpha = 1.0;           // Importancia de la feromona
    private double beta = 2.0;            // Importancia de la heurística
    private double rho = 0.5;             // Tasa de evaporación
    private double q0 = 0.9;              // Probabilidad de explotar vs explorar
    private int numeroHormigas = 50;
    private int numeroIteraciones = 100;
    private double feromonaInicial = 0.1;

    // Pesos para la función heurística
    private double pesoSla = 0.4;
    private double pesoCapacidad = 0.3;
    private double pesoTiempo = 0.3;
}
