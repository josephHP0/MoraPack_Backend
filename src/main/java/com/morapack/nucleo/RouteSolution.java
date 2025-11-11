package com.morapack.nucleo;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Solución interna del algoritmo ACO que representa una ruta construida por una hormiga.
 * Incluye la secuencia de vuelos y el costo heurístico acumulado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteSolution {

    /** Secuencia de instancias de vuelo que componen la solución */
    @Builder.Default
    private List<FlightInstance> path = new ArrayList<>();

    /** Costo heurístico total acumulado */
    private double totalCost;

    /** Indica si la solución es factible (cumple restricciones duras) */
    @Builder.Default
    private boolean factible = true;

    /**
     * Agrega un vuelo a la solución.
     *
     * @param flight Instancia de vuelo
     */
    public void agregarVuelo(FlightInstance flight) {
        if (path == null) {
            path = new ArrayList<>();
        }
        path.add(flight);
    }

    /**
     * Incrementa el costo total de la solución.
     *
     * @param costo Costo a sumar
     */
    public void agregarCosto(double costo) {
        this.totalCost += costo;
    }

    /**
     * Retorna el número de vuelos en la solución.
     *
     * @return Cantidad de vuelos
     */
    public int size() {
        return path != null ? path.size() : 0;
    }

    /**
     * Retorna el último vuelo agregado a la solución.
     *
     * @return Última FlightInstance, o null si no hay vuelos
     */
    public FlightInstance getUltimoVuelo() {
        return (path != null && !path.isEmpty())
            ? path.get(path.size() - 1)
            : null;
    }

    /**
     * Convierte la solución en una Ruta.
     *
     * @return Ruta correspondiente
     */
    public Ruta toRuta() {
        Ruta ruta = new Ruta();
        if (path != null) {
            for (FlightInstance flight : path) {
                ruta.agregarVuelo(flight);
            }
        }
        ruta.setCostoTotal(this.totalCost);
        return ruta;
    }

    @Override
    public String toString() {
        if (path == null || path.isEmpty()) {
            return "RouteSolution{empty}";
        }
        String origen = path.get(0).getOrigen();
        String destino = path.get(path.size() - 1).getDestino();
        return String.format("RouteSolution{%s->%s, hops=%d, cost=%.4f, factible=%b}",
            origen, destino, size(), totalCost, factible);
    }
}
