package com.morapack.dto;


import lombok.Data;

@Data
public class PedidoInputDto {
    private Integer dia;
    private Integer hora;
    private Integer minuto;
    private String icaoDestino;
    private Integer tamanho;
    private String nombreCliente;
}