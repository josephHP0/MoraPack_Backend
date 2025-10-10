package com.morapack.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para respuestas API.
 * - status: "success" o "error"
 * - mensaje: Descripción del resultado.
 * - data: Datos adicionales (Map, List, Object, o null).
 */
@Data
@NoArgsConstructor  // Constructor vacío (para JSON deserialización)
public class RespuestaDTO {
    private String status;  // "success" o "error"

    @NotBlank(message = "Mensaje requerido")
    private String mensaje;

    private Object data;  // Flexible: Map<String, Object>, List, etc.

    /**
     * Constructor principal: status, mensaje, data.
     * @param status "success" o "error"
     * @param mensaje Descripción
     * @param data Datos opcionales
     */
    public RespuestaDTO(String status, String mensaje, Object data) {
        this.status = status;
        this.mensaje = mensaje;
        this.data = data;
    }

    /**
     * Constructor simplificado: status y mensaje (data = null).
     */
    public RespuestaDTO(String status, String mensaje) {
        this(status, mensaje, null);
    }

    // Método helper para verificar si es éxito
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
