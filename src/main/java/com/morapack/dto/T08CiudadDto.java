package com.morapack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.morapack.model.T08Ciudad}
 */
@Value
public class T08CiudadDto implements Serializable {
    Integer id;
    @NotNull
    @Size(max = 80)
    String t08Nombre;
    @Size(max = 40)
    String t08Continente;
    @Size(max = 64)
    String t08Zonahoraria;
    Boolean t08Eshub;
}