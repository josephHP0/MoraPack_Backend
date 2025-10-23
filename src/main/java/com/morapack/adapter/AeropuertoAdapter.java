package com.morapack.adapter;

import com.morapack.model.T01Aeropuerto;

/**
 * Adapter para convertir T01Aeropuerto a la interfaz esperada por el planificador
 */
public class AeropuertoAdapter {
    private final T01Aeropuerto aeropuerto;

    public AeropuertoAdapter(T01Aeropuerto aeropuerto) {
        this.aeropuerto = aeropuerto;
    }

    public String getIata() {
        return aeropuerto.getT01Codigoicao();
    }

    public int getCapacidadAlmacen() {
        return aeropuerto.getT01Capacidad() != null ? aeropuerto.getT01Capacidad() : 0;
    }

    public boolean isInfiniteSource() {
        // Un aeropuerto se considera fuente infinita si su capacidad es null o muy grande
        return aeropuerto.getT01Capacidad() == null || aeropuerto.getT01Capacidad() >= Integer.MAX_VALUE / 4;
    }

    public T01Aeropuerto getEntity() {
        return aeropuerto;
    }
}