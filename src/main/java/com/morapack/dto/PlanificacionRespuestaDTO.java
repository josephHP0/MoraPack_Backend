package com.morapack.dto;

import java.util.List;
import lombok.Data;

@Data
public class PlanificacionRespuestaDTO {
    private Integer pedidoId;
    private String estado;
    private List<String> vuelos;
    private Double costoTotal;
}