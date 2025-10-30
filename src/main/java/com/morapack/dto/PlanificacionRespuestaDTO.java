package com.morapack.dto;

import java.util.List;
import lombok.Data;

@Data
public class PlanificacionRespuestaDTO {
    private Integer pedidoId;
    private String estado;

    /**
     * Lista de splits/chunks con sus rutas y cantidades.
     * Cada split representa un grupo de paquetes que sigue una ruta específica.
     */
    private List<SplitRutaDTO> splits;

    /**
     * @deprecated Usar splits en su lugar. Se mantiene por compatibilidad.
     */
    @Deprecated
    private List<String> vuelos;

    private Double costoTotal;
}