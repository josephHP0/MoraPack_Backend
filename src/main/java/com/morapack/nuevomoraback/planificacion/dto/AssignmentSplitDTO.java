package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSplitDTO {
    private String consignmentId;  // ID único del split/consignment
    private Integer qty;           // Cantidad total en este split
    private List<AssignmentLegDTO> legs;  // Tramos (vuelos) de este split
}
