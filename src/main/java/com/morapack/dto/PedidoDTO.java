package com.morapack.dto;

import lombok.Data;

// PedidoDTO.java
@Data
public class PedidoDTO {
    private String id;
    private int dia;
    private int hora;
    private int minuto;
    private String destino;
    private int tamano;
    private int minutoDisponible;
    private int slaDeadlineMin;
}

