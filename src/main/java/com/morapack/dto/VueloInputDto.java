package com.morapack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VueloInputDto {

    // 1. Campos ICAO (Vienen del archivo/formulario)
    @NotNull(message = "El ICAO de origen es requerido")
    @Pattern(regexp = "^[A-Z]{4}$", message = "El ICAO de origen debe ser de 4 letras mayúsculas")
    private String icaoOrigen;

    @NotNull(message = "El ICAO de destino es requerido")
    @Pattern(regexp = "^[A-Z]{4}$", message = "El ICAO de destino debe ser de 4 letras mayúsculas")
    private String icaoDestino;

    // 2. Campos de Hora (Vienen del archivo/formulario)
    @NotNull(message = "La hora de salida es requerida")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "La hora de salida debe ser HH:mm")
    private String horaSalidaStr;

    @NotNull(message = "La hora de llegada es requerida")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "La hora de llegada debe ser HH:mm")
    private String horaLlegadaStr;

    // 3. Capacidad (Viene del archivo/formulario)
    @NotNull(message = "La capacidad es requerida")
    @Min(value = 1, message = "La capacidad debe ser positiva")
    private Integer capacidad;

    // 4. Campos CALCULADOS (Seteados en UtilArchivos o DataService)
    // Se usan para pasar la hora a minutos del día para un mejor manejo lógico.
    private Integer hSalidaMinutos;
    private Integer hLlegadaMinutos;
}