package com.morapack.controller.NuevoPedido;

import com.morapack.dto.NuevoPedido.FiltroPedidoDTO;
import com.morapack.dto.RespuestaDTO;
import com.morapack.service.NuevoPedido.PedidosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión y consulta de nuevos pedidos.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class NuevoPedidoController {

    private final PedidosService pedidosService;

    /**
     * Endpoint: POST /api/v1/pedidos/filtrar
     * Permite filtrar pedidos por rango de fechas (obligatorio) y código ICAO (opcional).
     *
     * @param filtro DTO con las fechas de inicio y fin, y el código ICAO opcional.
     * @return RespuestaDTO con el estado y la lista de PedidoInternoDTO.
     */
    @PostMapping("/filtrar")
    public ResponseEntity<RespuestaDTO> filtrarPedidos(@Valid @RequestBody FiltroPedidoDTO filtro) {
        
        RespuestaDTO response = pedidosService.filtrarPedidos(filtro);
        
        // Retornamos 200 OK para éxito, info y warning, y 400 Bad Request para errores lógicos.
        if (response.isSuccess() || "info".equalsIgnoreCase(response.getStatus()) || "warning".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            // Errores de lógica de negocio (e.g., validación de rango de fechas en el servicio)
            return ResponseEntity.badRequest().body(response);
        }
    }
}