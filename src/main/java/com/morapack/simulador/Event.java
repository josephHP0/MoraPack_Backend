package com.morapack.simulador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Event {
    private final String pedidoId;
    private final String instanceId;
    private final String type;
    private final Instant time;
    private final int cantidad;
    private final String origen;
    private final String destino;

    public Event(String pedidoId, String instanceId, String type, Instant time, 
                int cantidad, String origen, String destino) {
        this.pedidoId = pedidoId;
        this.instanceId = instanceId;
        this.type = type;
        this.time = time;
        this.cantidad = cantidad;
        this.origen = origen;
        this.destino = destino;
    }

    public String getPedidoId() { return pedidoId; }
    public String getInstanceId() { return instanceId; }
    public String getType() { return type; }
    public Instant getTime() { return time; }
    public int getCantidad() { return cantidad; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
}