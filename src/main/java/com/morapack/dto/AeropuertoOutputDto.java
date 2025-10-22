package com.morapack.dto;

/**
 * DTO usado para enviar datos de aeropuertos al FRONT
 * (por ejemplo en el GET /api/aeropuertos).
 * Este formato coincide con el tipo AirportData que el front espera.
 */

public record AeropuertoOutputDto(
        String id,           // id del aeropuerto (en texto)
        String code,         // código ICAO (SPIM, UBBB, etc.)
        String name,         // alias o nombre visible
        String country,      // país o ciudad (placeholder)
        String abbreviation, // mismo código ICAO (por ahora)
        int utcOffset,       // desfase horario GMT
        int elevation,       // placeholder (no está en BD)
        String latitude,     // latitud en texto
        String longitude,    // longitud en texto
        String continent     // continente
) {}