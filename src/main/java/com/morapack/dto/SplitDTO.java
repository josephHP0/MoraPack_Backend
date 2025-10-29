package com.morapack.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO para representar un envío (split) de un pedido.
 * Un pedido puede dividirse en múltiples envíos que tomen rutas diferentes.
 */
@Data
public class SplitDTO {
    private String consignmentId;
    private Integer qty;
    private List<LegDTO> legs;
}
