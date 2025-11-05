package com.morapack.dto;

import java.util.List;
import lombok.Data;

/**
 * DTO para representar un split/chunk de un pedido con su ruta y cantidad.
 * Usado internamente entre PlanificadorAco y SemanalService.
 */
@Data
public class SplitRutaDTO {
    /**
     * Cantidad de paquetes en este split
     */
    private Integer cantidad;

    /**
     * Lista de IDs de vuelos que forman la ruta de este split.
     * Formato: "MP-{id}#{fechaSalida}"
     */
    private List<String> vuelosRuta;

    /**
     * Costo de este split (opcional, para métricas)
     */
    private Double costo;
}
