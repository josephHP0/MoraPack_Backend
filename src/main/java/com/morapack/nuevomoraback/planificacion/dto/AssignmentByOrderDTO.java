package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para representar las asignaciones de rutas agrupadas por pedido.
 * Este formato es compatible con el frontend para visualización en el mapa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentByOrderDTO {
    private String orderId;        // ID cadena del pedido (T02_ID_CADENA)
    private List<AssignmentSplitDTO> splits;  // Splits/rutas del pedido
}
