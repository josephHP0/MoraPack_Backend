package com.morapack.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO para solicitud de inicio de simulación semanal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionRequestDto {

    /**
     * Fecha y hora de inicio de la ventana de simulación (UTC).
     */
    @NotNull(message = "La fecha de inicio es obligatoria")
    private Instant fechaInicio;

    /**
     * Fecha y hora de fin de la ventana de simulación (UTC).
     */
    @NotNull(message = "La fecha de fin es obligatoria")
    private Instant fechaFin;

    /**
     * Lista de IDs de pedidos a simular.
     * Si es null o vacío, se simulan todos los pedidos en la ventana temporal.
     */
    private List<Integer> pedidoIds;

    /**
     * Lista de códigos ICAO de aeropuertos con capacidad infinita (hubs).
     * Si es null, se usan los hubs por defecto: SPIM, EBCI, UBBB.
     */
    private List<String> hubsInfinitos;

    /**
     * Si es true, se ejecuta la simulación de forma asíncrona.
     * Por defecto es true para simulaciones largas.
     */
    @Builder.Default
    private Boolean asincrona = true;
}
