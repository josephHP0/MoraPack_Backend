package com.morapack.simulador;

import java.util.ArrayList;
import java.util.List;

public class CancellationService {
    protected final TimelineStore store;

    public CancellationService(TimelineStore store) {
        this.store = store;
    }

    public CancellationSummary apply(List<CancellationRecord> records) {
        List<String> affectedPedidos = new ArrayList<>();
        
        for (CancellationRecord record : records) {
            // Marca pedidos afectados por la cancelación
            store.getEvents().stream()
                .filter(e -> e.getInstanceId().equals(record.getInstanceId()))
                .map(Event::getPedidoId)
                .distinct()
                .forEach(affectedPedidos::add);
        }
        
        return new CancellationSummary(affectedPedidos);
    }

    public static class CancellationSummary {
        private final List<String> affectedPedidos;

        public CancellationSummary(List<String> affectedPedidos) {
            this.affectedPedidos = affectedPedidos;
        }

        public List<String> getAffectedPedidos() {
            return affectedPedidos;
        }
    }
}