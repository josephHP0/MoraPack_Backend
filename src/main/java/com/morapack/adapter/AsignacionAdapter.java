package com.morapack.adapter;

import com.morapack.model.T09Asignacion;
import com.morapack.nucleo.Asignacion;
import com.morapack.nucleo.Ruta;

/**
 * Adaptador para convertir entre Asignacion del núcleo y T09Asignacion de la base de datos
 */
public class AsignacionAdapter {
    
    /**
     * Convierte una Asignacion del núcleo a una T09Asignacion de la base de datos
     */
    public static T09Asignacion toEntity(Asignacion asignacion) {
        T09Asignacion entity = new T09Asignacion();
        entity.setT03Idpedido(asignacion.getPedido());
        entity.setPaquetesAsignados(asignacion.getPaquetesAsignados());
        entity.setT09Estadoasignacion("PENDIENTE");
        entity.setT09Orden(1);
        
        // Copiar la ruta si existe
        if (asignacion.getRuta() != null) {
            entity.setRuta(asignacion.getRuta());
        }
        
        return entity;
    }
    
    /**
     * Convierte una lista de Asignacion del núcleo a una lista de T09Asignacion
     */
    public static java.util.List<T09Asignacion> toEntityList(java.util.List<Asignacion> asignaciones) {
        return asignaciones.stream()
                .map(AsignacionAdapter::toEntity)
                .collect(java.util.stream.Collectors.toList());
    }
}