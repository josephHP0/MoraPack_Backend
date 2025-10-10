package com.morapack.dto;

import lombok.Data;

// VueloDTO.java
@Data
public class VueloDTO {
    private String origen;
    private String destino;
    private int salidaMin;  // Minutos UTC
    private int llegadaMin;
    private int capacidad;
}
