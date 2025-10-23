package com.morapack.util;

import com.morapack.planificador.api.model.PlanResponse;
import com.morapack.planificador.nucleo.Asignacion;
import com.morapack.planificador.nucleo.Ruta;
import com.morapack.planificador.simulation.EstadosTemporales;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanConverter {

    public static List<PlanResponse> convertToPlanResponse(EstadosTemporales estadosTemporales) {
        Map<String, PlanResponse> responseMap = new HashMap<>();

        // Procesar las asignaciones y rutas
        for (Map.Entry<String, String> entry : estadosTemporales.getEstadoPedido().entrySet()) {
            String orderId = entry.getKey();

            // Crear PlanResponse si no existe
            responseMap.putIfAbsent(orderId, new PlanResponse());
            PlanResponse response = responseMap.get(orderId);
            response.setOrderId(orderId);
            response.setSplits(new ArrayList<>());
        }

        // TODO: Completar la lógica de conversión accediendo a las asignaciones
        // y rutas del plan a través de EstadosTemporales

        return new ArrayList<>(responseMap.values());
    }

    private static List<PlanResponse.Leg> convertToLegs(List<Ruta.Tramo> tramos) {
        List<PlanResponse.Leg> legs = new ArrayList<>();
        int seq = 1;

        for (Ruta.Tramo tramo : tramos) {
            PlanResponse.Leg leg = new PlanResponse.Leg();
            leg.setSeq(seq++);
            leg.setInstanceId(tramo.getInstanceId());
            leg.setFrom(tramo.getFrom());
            leg.setTo(tramo.getTo());
            leg.setQty(tramo.getQty());
            legs.add(leg);
        }

        return legs;
    }
}