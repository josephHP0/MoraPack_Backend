package com.morapack.dto.NuevoPedido;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;

/**
 * DTO para la solicitud de búsqueda de pedidos con filtros.
 * Fecha de inicio y fin son obligatorias; código ICAO es opcional.
 */
@Data
public class FiltroPedidoDTO {

    @NotNull(message = "La fecha de inicio (fechaInicio) es obligatoria.")
    private Instant fechaInicio;

    @NotNull(message = "La fecha de fin (fechaFin) es obligatoria.")
    private Instant fechaFin;

    // Código ICAO es opcional. Si se provee, filtra por aeropuerto destino.
    private String icaoCodigo;
}