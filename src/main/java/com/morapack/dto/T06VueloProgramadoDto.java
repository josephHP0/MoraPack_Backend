package com.morapack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link com.morapack.model.T06VueloProgramado}
 * Incluye los códigos ICAO de origen y destino para la salida.
 */
@Value
public class T06VueloProgramadoDto implements Serializable {
    Integer id;
    Instant t06Fechasalida;
    Instant t06Fechallegada;
    Integer t06Capacidadtotal;
    Integer t06Ocupacionactual;
    String t06Estado;
    String t06Estadocapacidad;

    // *** CAMPOS AÑADIDOS PARA RESOLVER EL ERROR DE CONSTRUCTOR ***
    // (Estos son los datos que el servicio intentaba pasar al constructor)
    String t01CodigoicaoOrigen;
    String t01CodigoicaoDestino;
}