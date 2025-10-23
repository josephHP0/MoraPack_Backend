package com.morapack.dto;

/**
 * DTO usado para enviar datos de aeropuertos al FRONT
 * (por ejemplo en el GET /api/aeropuertos).
 * Este formato coincide con el tipo AirportICAO que el front espera.
 */

public record AeropuertoOutputDto(
        String icao,         // código ICAO (SPIM, UBBB, etc.)
        String iata,         // código IATA (opcional)
        String name,         // alias o nombre visible
        String city,         // ciudad
        String country,      // país
        double lat,          // latitud en double
        double lon,          // longitud en double
        Integer warehouseCapacity,  // capacidad de almacén (opcional)
        Boolean infiniteSource      // si es fuente infinita (hub)
) {}