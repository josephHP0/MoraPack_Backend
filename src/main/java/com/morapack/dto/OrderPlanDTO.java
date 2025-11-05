package com.morapack.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO para la planificación de pedidos.
 * Representa un pedido con sus envíos y rutas asignadas.
 */
@Data
public class OrderPlanDTO {
    private String orderId;
    private List<SplitDTO> splits;
}
