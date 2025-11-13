package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Ant {

    private Map<T02Pedido, List<T04VueloProgramado>> solucion = new HashMap<>();
    private double costoTotal = 0.0;

    public void asignarVuelo(T02Pedido pedido, T04VueloProgramado vuelo) {
        solucion.computeIfAbsent(pedido, k -> new ArrayList<>()).add(vuelo);
    }

    public void calcularCosto() {
        // Simplificado: en implementación real, calcular costo basado en SLA, ocupación, etc.
        costoTotal = solucion.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
