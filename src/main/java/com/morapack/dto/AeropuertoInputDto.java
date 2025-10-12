package com.morapack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AeropuertoInputDto {
    @NotBlank(message = "Nombre de ciudad requerido")
    @Size(max = 80)
    private String nombreCiudad;  // Para T08Ciudad.t08Nombre (unique)

    @Size(max = 40)
    private String continenteCiudad;  // T08Ciudad.t08Continente (AM/AS/EU)

    @Size(max = 40)
    private String paisCiudad;  // T08Ciudad.t08Continente (AM/AS/EU)

    @Size(max = 64)
    private String zonaHorariaCiudad;  // T08Ciudad.t08Zonahoraria (default "UTC")

    @NotNull(message = "esHub requerido")
    private Boolean esHubCiudad;  // T08Ciudad.t08Eshub (default false)

    @NotBlank(message = "Código ICAO requerido")
    @Size(max = 4, min = 4)
    @Pattern(regexp = "[A-Z]{4}", message = "ICAO debe ser 4 letras mayúsculas")
    private String codigoICAO;  // T01Aeropuerto.t01Codigoicao (unique)

    @Size(max = 24)
    @Pattern(regexp = "^\\d{1,3}°\\d{1,2}'\\d{1,2}\"[NSEW]$", message = "Formato DMS inválido latDms")
    private String latDms;  // DMS crudo para parseo

    @Size(max = 24)
    @Pattern(regexp = "^\\d{1,3}°\\d{1,2}'\\d{1,2}\"[NSEW]$", message = "Formato DMS inválido lonDms")
    private String lonDms;  // DMS crudo para parseo

    @NotNull(message = "GMT Offset requerido")
    private Short gmtOffset;  // T01Aeropuerto.t01GmtOffset

    @NotNull(message = "Capacidad requerida")
    @Min(value = 0, message = "Capacidad debe ser >= 0")
    private Integer capacidad;  // T01Aeropuerto.t01Capacidad

    @Size(max = 40)
    private String alias;  // T01Aeropuerto.t01Alias

    // Asegúrate de tener sus setters/getters
    private BigDecimal lat;
    private BigDecimal lon;

}
