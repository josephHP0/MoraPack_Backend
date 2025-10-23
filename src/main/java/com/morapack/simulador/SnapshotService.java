package com.morapack.simulador;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class SnapshotService {
    protected final List<Event> events;
    protected final Map<String, Integer> capacidadAeropuertos;
    protected final Map<String, Integer> capacidadVuelos;

    public SnapshotService(List<Event> events,
                          Map<String, com.morapack.model.T01Aeropuerto> aeropuertos,
                          Map<String, Integer> capacidadVuelos) {
        this.events = new ArrayList<>(events);
        this.capacidadVuelos = new HashMap<>(capacidadVuelos);
        this.capacidadAeropuertos = new HashMap<>();
        
        // Mapear capacidades de aeropuertos
        aeropuertos.forEach((code, ap) -> 
            this.capacidadAeropuertos.put(code, ap.getT01Capacidad())
        );
    }

    public com.morapack.service.EstadosTemporales stateAt(Instant at) {
        Map<String, Integer> ocupacionAeropuerto = new HashMap<>();
        Map<String, Integer> ocupacionVueloInstance = new HashMap<>();
        Map<String, List<String>> pedidosEnAeropuerto = new HashMap<>();
        Map<String, String> estadoPedido = new HashMap<>();

        for (Event e : events) {
            if (e.getTime().isAfter(at)) break;

            updateState(e, ocupacionAeropuerto, ocupacionVueloInstance, 
                       pedidosEnAeropuerto, estadoPedido);
        }

        return new com.morapack.service.EstadosTemporales(
            at, ocupacionAeropuerto, ocupacionVueloInstance, 
            pedidosEnAeropuerto, estadoPedido
        );
    }

    protected void updateState(Event e,
                             Map<String, Integer> ocupacionAeropuerto,
                             Map<String, Integer> ocupacionVueloInstance,
                             Map<String, List<String>> pedidosEnAeropuerto,
                             Map<String, String> estadoPedido) {
        switch (e.getType()) {
            case "DEPARTURE":
                // Reducir ocupación en origen
                ocupacionAeropuerto.merge(e.getOrigen(), -e.getCantidad(), Integer::sum);
                // Aumentar ocupación en vuelo
                ocupacionVueloInstance.merge(e.getInstanceId(), e.getCantidad(), Integer::sum);
                // Remover pedido del aeropuerto origen
                removePedidoFromAeropuerto(e.getPedidoId(), e.getOrigen(), pedidosEnAeropuerto);
                // Actualizar estado del pedido
                estadoPedido.put(e.getPedidoId(), "EN_VUELO");
                break;

            case "ARRIVAL":
                // Reducir ocupación en vuelo
                ocupacionVueloInstance.merge(e.getInstanceId(), -e.getCantidad(), Integer::sum);
                // Aumentar ocupación en destino
                ocupacionAeropuerto.merge(e.getDestino(), e.getCantidad(), Integer::sum);
                // Agregar pedido al aeropuerto destino
                pedidosEnAeropuerto.computeIfAbsent(e.getDestino(), k -> new ArrayList<>())
                                 .add(e.getPedidoId());
                // Actualizar estado del pedido
                estadoPedido.put(e.getPedidoId(), "EN_ALMACEN");
                break;
        }
    }

    private void removePedidoFromAeropuerto(String pedidoId, String aeropuertoId,
                                          Map<String, List<String>> pedidosEnAeropuerto) {
        List<String> pedidos = pedidosEnAeropuerto.get(aeropuertoId);
        if (pedidos != null) {
            pedidos.remove(pedidoId);
            if (pedidos.isEmpty()) {
                pedidosEnAeropuerto.remove(aeropuertoId);
            }
        }
    }
}