package com.morapack.nucleo;

import lombok.*;

import java.time.Instant;

/**
 * Representa un tramo individual de una ruta logística.
 * Vincula un vuelo específico (FlightInstance) con sus tiempos de salida/llegada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Tramo {

    /** ID único de la instancia de vuelo */
    private String instanceId;

    /** ID de la entidad T06VueloProgramado */
    private Integer vueloId;

    /** Código ICAO del aeropuerto de origen */
    private String origen;

    /** Código ICAO del aeropuerto de destino */
    private String destino;

    /** Hora de salida en UTC */
    private Instant depUtc;

    /** Hora de llegada en UTC */
    private Instant arrUtc;

    /**
     * Calcula la duración del tramo en horas.
     *
     * @return Duración en horas
     */
    public double getDuracionHoras() {
        if (depUtc == null || arrUtc == null) {
            return 0.0;
        }
        long seconds = arrUtc.getEpochSecond() - depUtc.getEpochSecond();
        return seconds / 3600.0;
    }

    /**
     * Crea un Tramo a partir de una FlightInstance.
     *
     * @param flight Instancia de vuelo
     * @return Tramo correspondiente
     */
    public static Tramo fromFlightInstance(FlightInstance flight) {
        return Tramo.builder()
            .instanceId(flight.getInstanceId())
            .vueloId(flight.getVueloId())
            .origen(flight.getOrigen())
            .destino(flight.getDestino())
            .depUtc(flight.getDepUtc())
            .arrUtc(flight.getArrUtc())
            .build();
    }
}
