package com.morapack.nuevomoraback.common.service;

import com.morapack.nuevomoraback.common.dto.AeropuertoDTO;

import java.util.List;

public interface AeropuertoService {

    /**
     * Obtiene todos los aeropuertos disponibles
     * @return Lista de aeropuertos con sus coordenadas y datos
     */
    List<AeropuertoDTO> obtenerTodosLosAeropuertos();

    /**
     * Obtiene solo los aeropuertos HUB (Lima, Bruselas, Bakú)
     * @return Lista de aeropuertos hub
     */
    List<AeropuertoDTO> obtenerAeropuertosHub();

    /**
     * Obtiene solo los aeropuertos NO-HUB (destinos finales)
     * @return Lista de aeropuertos no-hub
     */
    List<AeropuertoDTO> obtenerAeropuertosNoHub();
}
