package com.morapack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AeropuertoDTO {

    private Integer idAeropuerto;

    @NotNull(message = "ID de ciudad requerido (FK a T08Ciudad)")
    private String idCiudad;  // t01Idciudad (String para flexibilidad; ajusta a Integer si es numérico)

    @NotBlank(message = "Código ICAO requerido")
    @Size(max = 4, min = 4)
    @Pattern(regexp = "[A-Z]{4}", message = "ICAO debe ser 4 letras mayúsculas")
    private String codigoICAO;  // t01Codigoicao (placeholder si no en formato)

    @NotBlank(message = "Nombre de ciudad requerido")
    @Size(max = 80)
    private String ciudadNombre;  // t01Ciudadnombre

    @NotBlank(message = "País requerido")
    @Size(max = 80)
    private String pais;  // t01Pais

    @Size(max = 10)
    private String alias;  // t01Alias (ej. "bogo")

    @NotBlank(message = "Continente requerido")
    @Size(max = 40)
    @Pattern(regexp = "AM|AS|EU", message = "Continente debe ser AM, AS o EU")
    private String continente;  // t01Continente

    @NotNull(message = "GMT Offset requerido")
    private Byte gmtOffset;  // t01GmtOffset (ej. -5)

    @NotNull(message = "Capacidad requerida")
    private Short capacidad;  // t01Capacidad

    @Size(max = 24)
    @Pattern(regexp = "\\d+°\\d+'\\d+\"[NSEW]", message = "Formato DMS inválido para latDms (ej. 35°59'36\"N)")
    private String latDms;  // Ej. "04°42'05\"N" o "35°59'36\"E" (sin espacios)
    @Size(max = 24)
    @Pattern(regexp = "\\d+°\\d+'\\d+\"[NSEW]", message = "Formato DMS inválido para lonDms (ej. 35°59'36\"E)")
    private String lonDms;  // Ej. "74°08'49\"W"

    // Campos calculados (se setean en service desde DMS)
    private BigDecimal lat;  // t01Lat
    private BigDecimal lon;  // t01Lon
}
