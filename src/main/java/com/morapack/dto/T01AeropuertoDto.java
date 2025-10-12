package com.morapack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link com.morapack.model.T01Aeropuerto}
 */
@Value
public class T01AeropuertoDto implements Serializable {
    Integer id;
    @NotNull
    @Size(max = 4)
    String t01Codigoicao;
    BigDecimal t01Lat;
    BigDecimal t01Lon;
    Short t01GmtOffset;
    Integer t01Capacidad;
    @Size(max = 40)
    String t01Alias;
}