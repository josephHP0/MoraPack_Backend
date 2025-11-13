package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T01Aeropuerto;
import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.domain.T05Ciudad;
import com.morapack.nuevomoraback.common.repository.AeropuertoRepository;
import com.morapack.nuevomoraback.common.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidadorReglas {

    private final AeropuertoRepository aeropuertoRepository;
    private final CiudadRepository ciudadRepository;

    /**
     * Valida si un pedido debe ser planificado (no es destino hub/almacén principal)
     */
    public boolean esDestinoValido(T02Pedido pedido) {
        T01Aeropuerto destino = pedido.getT02IdAeropDestino();
        List<T05Ciudad> hubs = ciudadRepository.findByT05EsHub(true);

        return hubs.stream()
                .noneMatch(hub -> hub.getId().equals(destino.getT01IdCiudad()));
    }

    /**
     * Valida si un vuelo tiene capacidad suficiente
     */
    public boolean tieneCapacidad(T04VueloProgramado vuelo, Integer cantidadProductos) {
        int disponible = vuelo.getT04CapacidadTotal() - vuelo.getT04OcupacionTotal();
        return disponible >= cantidadProductos;
    }

    /**
     * Valida si el vuelo respeta el tiempo mínimo de estancia (1 hora)
     */
    public boolean respetaTiempoEstancia(T04VueloProgramado vueloAnterior,
                                          T04VueloProgramado vueloSiguiente) {
        Instant llegadaAnterior = vueloAnterior.getT04FechaLlegada();
        Instant salidaSiguiente = vueloSiguiente.getT04FechaSalida();

        long horasEstancia = Duration.between(llegadaAnterior, salidaSiguiente).toHours();
        return horasEstancia >= 1;
    }

    /**
     * Valida que cantidad de productos esté en rango [1, 999]
     */
    public boolean cantidadValida(Integer cantidad) {
        return cantidad != null && cantidad >= 1 && cantidad <= 999;
    }
}
