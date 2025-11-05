package com.morapack.nucleo;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T06VueloProgramado;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class GrafoVuelos {
    private final Map<Integer, T01Aeropuerto> aeropuertos;
    private final Map<Integer, List<T06VueloProgramado>> vuelosPorOrigen;
    private final Map<Integer, Integer> ocupacionAeropuerto;
    
    public GrafoVuelos() {
        this.aeropuertos = new HashMap<>();
        this.vuelosPorOrigen = new HashMap<>();
        this.ocupacionAeropuerto = new HashMap<>();
    }

    public void agregarAeropuerto(T01Aeropuerto aeropuerto) {
        aeropuertos.put(aeropuerto.getId(), aeropuerto);
        vuelosPorOrigen.putIfAbsent(aeropuerto.getId(), new ArrayList<>());
        ocupacionAeropuerto.put(aeropuerto.getId(), 0);
    }

    public void agregarVuelo(T06VueloProgramado vuelo) {
        vuelosPorOrigen.get(vuelo.getT01Idaeropuertoorigen().getId()).add(vuelo);
    }

    public List<T06VueloProgramado> obtenerVuelosDesde(Integer aeropuertoId, Instant desde, Instant hasta) {
        return vuelosPorOrigen.get(aeropuertoId).stream()
                .filter(v -> !v.getT06Fechasalida().isBefore(desde) && !v.getT06Fechasalida().isAfter(hasta))
                .toList();
    }

    public boolean validarCapacidadAeropuerto(T01Aeropuerto aeropuerto, int cantidadPaquetes) {
        int ocupacionActual = ocupacionAeropuerto.get(aeropuerto.getId());
        return ocupacionActual + cantidadPaquetes <= aeropuerto.getT01Capacidad();
    }

    public boolean validarCapacidadVuelo(T06VueloProgramado vuelo, int cantidadPaquetes) {
        return vuelo.getT06Ocupacionactual() + cantidadPaquetes <= vuelo.getT06Capacidadtotal();
    }

    public void actualizarOcupacionAeropuerto(T01Aeropuerto aeropuerto, int delta) {
        int ocupacionActual = ocupacionAeropuerto.get(aeropuerto.getId());
        ocupacionAeropuerto.put(aeropuerto.getId(), ocupacionActual + delta);
    }

    public boolean sonMismoContinente(T01Aeropuerto origen, T01Aeropuerto destino) {
        String continenteOrigen = origen.getT08Idciudad().getT08Continente();
        String continenteDestino = destino.getT08Idciudad().getT08Continente();
        if (continenteOrigen == null || continenteDestino == null) {
            return false;
        }
        if (!continenteOrigen.equals(continenteDestino)) {
            return false;
        }
        return true;
    }

    public Duration calcularTiempoTransito(T01Aeropuerto origen, T01Aeropuerto destino) {
        if (sonMismoContinente(origen, destino)) {
            return Duration.ofHours(12); // Medio día para mismo continente
        } else {
            return Duration.ofDays(1); // Un día para distinto continente
        }
    }

    public List<T06VueloProgramado> obtenerVuelosEnPeriodo(Instant desde, Instant hasta) {
        List<T06VueloProgramado> vuelos = new ArrayList<>();
        for (List<T06VueloProgramado> vuelosOrigen : vuelosPorOrigen.values()) {
            for (T06VueloProgramado vuelo : vuelosOrigen) {
                if (!vuelo.getT06Fechasalida().isBefore(desde) && 
                    !vuelo.getT06Fechasalida().isAfter(hasta)) {
                    vuelos.add(vuelo);
                }
            }
        }
        return vuelos;
    }

    public Duration calcularTiempoEntrega(T01Aeropuerto destino) {
        return Duration.ofHours(2); // 2 horas para entrega en destino
    }

    public Map<Integer, T01Aeropuerto> getAeropuertos() {
        return aeropuertos;
    }

    public boolean esAlmacenInfinito(T01Aeropuerto aeropuerto) {
        return "SPIM".equals(aeropuerto.getT01Codigoicao()) || // Lima
               "EBBR".equals(aeropuerto.getT01Codigoicao()) || // Bruselas
               "UBBB".equals(aeropuerto.getT01Codigoicao());   // Baku
    }
}
