package com.morapack.dto;

import lombok.Data;
import java.time.Instant;

/**
 * DTO para representar una cancelación de vuelo.
 * El archivo de cancelaciones contiene: día, hora, ID vuelo, origen, destino
 */
@Data
public class CancelacionDTO {
    private Integer dia;              // Día del mes
    private String hora;              // Hora en formato HH:mm
    private Integer idVuelo;          // ID del vuelo (ej: 1 para MP-001)
    private String icaoOrigen;        // Código ICAO del aeropuerto origen
    private String icaoDestino;       // Código ICAO del aeropuerto destino
    private Instant fechaHoraCancelacion; // Fecha y hora calculada de la cancelación
}
