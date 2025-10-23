package com.morapack.nucleo;

import com.morapack.model.T03Pedido;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resultado de planificación para un pedido (T03Pedido) o subgrupo del pedido.
 * Mantiene la información de asignación y ruta para construcción del timeline.
 */
public class Asignacion {
    private T03Pedido pedido;
    
    /** ICAO del aeropuerto de origen si es un hub infinito */
    private String hubOrigen;
    
    /** Ruta temporalizada completa */
    private Ruta ruta;
    
    /** Cantidad de paquetes asignados a esta ruta */
    private int paquetesAsignados;
    
    /** Cantidad de paquetes pendientes de asignar */
    private int paquetesPendientes;

    public Asignacion() {}

    public Asignacion(T03Pedido pedido, String hubOrigen, Ruta ruta, int paquetesAsignados, int paquetesPendientes) {
        this.pedido = pedido;
        this.hubOrigen = hubOrigen;
        this.ruta = ruta;
        this.paquetesAsignados = paquetesAsignados;
        this.paquetesPendientes = paquetesPendientes;
    }

    /** Obtiene la lista de tramos de la ruta */
    public List<Ruta.Tramo> getTramos() {
        return (ruta != null) ? ruta.tramos : Collections.emptyList();
    }

    // Getters y setters
    public T03Pedido getPedido() { return pedido; }
    public void setPedido(T03Pedido pedido) { this.pedido = pedido; }
    
    public String getHubOrigen() { return hubOrigen; }
    public void setHubOrigen(String hubOrigen) { this.hubOrigen = hubOrigen; }
    
    public Ruta getRuta() { return ruta; }
    public void setRuta(Ruta ruta) { this.ruta = ruta; }
    
    public int getPaquetesAsignados() { return paquetesAsignados; }
    public void setPaquetesAsignados(int paquetesAsignados) { this.paquetesAsignados = paquetesAsignados; }
    
    public int getPaquetesPendientes() { return paquetesPendientes; }
    public void setPaquetesPendientes(int paquetesPendientes) { this.paquetesPendientes = paquetesPendientes; }

    /** Verifica si el pedido tiene una asignación válida */
    public boolean tieneAsignacion() { return paquetesAsignados > 0 && ruta != null && !getTramos().isEmpty(); }

    @Override
    public String toString() {
        return "Asignacion{pedido=" + (pedido != null ? pedido.getId() : "null") +
                ", hubOrigen=" + hubOrigen +
                ", asignados=" + paquetesAsignados +
                ", pendientes=" + paquetesPendientes +
                ", tramos=" + getTramos().size() + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(pedido != null ? pedido.getId() : null, hubOrigen, 
                          paquetesAsignados, paquetesPendientes);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Asignacion other)) return false;
        return Objects.equals(this.pedido != null ? this.pedido.getId() : null, 
                            other.pedido != null ? other.pedido.getId() : null)
                && Objects.equals(this.hubOrigen, other.hubOrigen)
                && this.paquetesAsignados == other.paquetesAsignados
                && this.paquetesPendientes == other.paquetesPendientes
                && Objects.equals(this.ruta, other.ruta);
    }
}

