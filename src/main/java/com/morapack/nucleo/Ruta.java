package com.morapack.nucleo;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una ruta completa de transporte logístico compuesta por múltiples tramos.
 * Una ruta conecta un origen con un destino a través de 1 o más vuelos (con posibles escalas).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ruta {

    /** Lista de tramos que componen la ruta (ordenados cronológicamente) */
    @Builder.Default
    private List<Tramo> tramos = new ArrayList<>();

    /** Costo heurístico total de la ruta (para comparación ACO) */
    private double costoTotal;

    // ========== Métodos de Construcción ==========

    /**
     * Agrega un tramo al final de la ruta.
     *
     * @param tramo Tramo a agregar
     */
    public void agregarTramo(Tramo tramo) {
        if (tramos == null) {
            tramos = new ArrayList<>();
        }
        tramos.add(tramo);
    }

    /**
     * Agrega un tramo creado a partir de una FlightInstance.
     *
     * @param flight Instancia de vuelo
     */
    public void agregarVuelo(FlightInstance flight) {
        agregarTramo(Tramo.fromFlightInstance(flight));
    }

    // ========== Métodos de Consulta ==========

    /**
     * Retorna el número de saltos (tramos) de la ruta.
     *
     * @return Cantidad de tramos
     */
    public int getNumSaltos() {
        return tramos != null ? tramos.size() : 0;
    }

    /**
     * Retorna el aeropuerto de origen de la ruta.
     *
     * @return Código ICAO del origen, o null si no hay tramos
     */
    public String getOrigen() {
        return (tramos != null && !tramos.isEmpty()) ? tramos.get(0).getOrigen() : null;
    }

    /**
     * Retorna el aeropuerto de destino de la ruta.
     *
     * @return Código ICAO del destino, o null si no hay tramos
     */
    public String getDestino() {
        return (tramos != null && !tramos.isEmpty())
            ? tramos.get(tramos.size() - 1).getDestino()
            : null;
    }

    /**
     * Retorna la hora de salida del primer tramo.
     *
     * @return Instant de salida, o null si no hay tramos
     */
    public Instant getFechaSalida() {
        return (tramos != null && !tramos.isEmpty()) ? tramos.get(0).getDepUtc() : null;
    }

    /**
     * Retorna la hora de llegada del último tramo.
     *
     * @return Instant de llegada, o null si no hay tramos
     */
    public Instant getFechaLlegada() {
        return (tramos != null && !tramos.isEmpty())
            ? tramos.get(tramos.size() - 1).getArrUtc()
            : null;
    }

    /**
     * Calcula la duración total de la ruta en horas (incluyendo esperas).
     *
     * @return Duración total en horas
     */
    public double getDuracionTotalHoras() {
        Instant salida = getFechaSalida();
        Instant llegada = getFechaLlegada();
        if (salida == null || llegada == null) {
            return 0.0;
        }
        long seconds = llegada.getEpochSecond() - salida.getEpochSecond();
        return seconds / 3600.0;
    }

    /**
     * Calcula el tiempo total de vuelo (sin contar esperas).
     *
     * @return Tiempo de vuelo en horas
     */
    public double getTiempoVueloHoras() {
        if (tramos == null) {
            return 0.0;
        }
        return tramos.stream()
            .mapToDouble(Tramo::getDuracionHoras)
            .sum();
    }

    /**
     * Calcula el tiempo total de espera en aeropuertos intermedios.
     *
     * @return Tiempo de espera en horas
     */
    public double getTiempoEsperaHoras() {
        return getDuracionTotalHoras() - getTiempoVueloHoras();
    }

    /**
     * Retorna la lista de aeropuertos visitados (incluyendo origen y destino).
     *
     * @return Lista de códigos ICAO
     */
    public List<String> getAeropuertosVisitados() {
        if (tramos == null || tramos.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> aeropuertos = new ArrayList<>();
        aeropuertos.add(tramos.get(0).getOrigen());
        for (Tramo tramo : tramos) {
            aeropuertos.add(tramo.getDestino());
        }
        return aeropuertos;
    }

    /**
     * Retorna una representación textual del itinerario.
     *
     * @return String con formato "ORIG -> HUB1 -> HUB2 -> DEST"
     */
    public String getItinerario() {
        if (tramos == null || tramos.isEmpty()) {
            return "";
        }
        return getAeropuertosVisitados().stream()
            .collect(Collectors.joining(" -> "));
    }

    /**
     * Verifica si la ruta es válida (tiene al menos un tramo y conectividad correcta).
     *
     * @return true si la ruta es válida
     */
    public boolean esValida() {
        if (tramos == null || tramos.isEmpty()) {
            return false;
        }

        // Verificar conectividad entre tramos consecutivos
        for (int i = 0; i < tramos.size() - 1; i++) {
            Tramo actual = tramos.get(i);
            Tramo siguiente = tramos.get(i + 1);

            // El destino del tramo actual debe ser el origen del siguiente
            if (!actual.getDestino().equals(siguiente.getOrigen())) {
                return false;
            }

            // El siguiente tramo debe salir después de la llegada del actual
            if (siguiente.getDepUtc().isBefore(actual.getArrUtc())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Crea una copia profunda de la ruta.
     *
     * @return Nueva instancia con los mismos valores
     */
    public Ruta copy() {
        List<Tramo> tramosCopia = new ArrayList<>();
        if (tramos != null) {
            for (Tramo t : tramos) {
                tramosCopia.add(Tramo.builder()
                    .instanceId(t.getInstanceId())
                    .vueloId(t.getVueloId())
                    .origen(t.getOrigen())
                    .destino(t.getDestino())
                    .depUtc(t.getDepUtc())
                    .arrUtc(t.getArrUtc())
                    .build());
            }
        }
        return Ruta.builder()
            .tramos(tramosCopia)
            .costoTotal(this.costoTotal)
            .build();
    }

    @Override
    public String toString() {
        return String.format("Ruta{%s, saltos=%d, duracion=%.2fh, costo=%.4f}",
            getItinerario(), getNumSaltos(), getDuracionTotalHoras(), costoTotal);
    }
}
