package com.morapack.adapter;

import com.morapack.model.T06VueloProgramado;
import java.time.Duration;
import java.time.Instant;

/**
 * Adapter para convertir T06VueloProgramado a la interfaz esperada por el planificador
 */
public class VueloProgramadoAdapter {
    private final T06VueloProgramado vuelo;

    public VueloProgramadoAdapter(T06VueloProgramado vuelo) {
        this.vuelo = vuelo;
    }

    public String getOrigen() {
        return vuelo.getT01Idaeropuertoorigen().getT01Codigoicao();
    }

    public String getDestino() {
        return vuelo.getT01Idaeropuertodestino().getT01Codigoicao();
    }

    public int getCapacidad() {
        return vuelo.getT06Capacidadtotal() != null ? vuelo.getT06Capacidadtotal() : 0;
    }

    public int getDurationMinutes() {
        if (vuelo.getT06Fechasalida() != null && vuelo.getT06Fechallegada() != null) {
            return (int) Duration.between(vuelo.getT06Fechasalida(), vuelo.getT06Fechallegada()).toMinutes();
        }
        return 0;
    }

    public Instant getSalidaUtc() {
        return vuelo.getT06Fechasalida();
    }

    public Instant getLlegadaUtc() {
        return vuelo.getT06Fechallegada();
    }

    public boolean isCancelado() {
        return "CANCELADO".equals(vuelo.getT06Estado());
    }

    public int getOcupacionActual() {
        return vuelo.getT06Ocupacionactual() != null ? vuelo.getT06Ocupacionactual() : 0;
    }

    public T06VueloProgramado getEntity() {
        return vuelo;
    }
}