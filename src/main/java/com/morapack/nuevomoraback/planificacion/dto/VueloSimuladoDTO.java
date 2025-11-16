package com.morapack.nuevomoraback.planificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO simplificado de vuelo para visualización en tiempo real de la simulación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VueloSimuladoDTO {

    private Integer id;
    private String codigoOrigenICAO;
    private String nombreOrigenCiudad;
    private String codigoDestinoICAO;
    private String nombreDestinoCiudad;
    private Instant fechaSalida;
    private Instant fechaLlegada;
    private Integer capacidadTotal;
    private Integer capacidadOcupada;
    private Integer capacidadDisponible;
    private String estado;
}
