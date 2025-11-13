package com.morapack.nuevomoraback.planificacion.aco;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PheromoneMatrix {

    @Getter
    private Map<String, Double> feromonas = new HashMap<>();
    private final AcoParameters params;

    public PheromoneMatrix(AcoParameters params) {
        this.params = params;
    }

    public void inicializar() {
        feromonas.clear();
    }

    public double obtenerFeromona(Integer pedidoId, Integer vueloId) {
        String clave = generarClave(pedidoId, vueloId);
        return feromonas.getOrDefault(clave, params.getFeromonaInicial());
    }

    public void actualizarFeromona(Integer pedidoId, Integer vueloId, double cantidad) {
        String clave = generarClave(pedidoId, vueloId);
        double actual = obtenerFeromona(pedidoId, vueloId);
        feromonas.put(clave, actual + cantidad);
    }

    public void evaporar() {
        feromonas.replaceAll((k, v) -> v * (1.0 - params.getRho()));
    }

    private String generarClave(Integer pedidoId, Integer vueloId) {
        return pedidoId + "_" + vueloId;
    }
}
