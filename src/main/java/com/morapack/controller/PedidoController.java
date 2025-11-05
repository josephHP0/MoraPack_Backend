package com.morapack.controller;

import com.morapack.dto.PedidoInputDto;
import com.morapack.dto.RespuestaDTO;
import com.morapack.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    // --------------------------------------------------------------------
    // ENDPOINTS DE ALTA (POST)
    // --------------------------------------------------------------------

    /** * POST /api/pedidos/formulario
     * Agrega un pedido individualmente.
     */
    @PostMapping("/formulario")
    public ResponseEntity<RespuestaDTO> cargarPedidoFormulario(
            @Valid @RequestBody PedidoInputDto inputDto,
            BindingResult bindingResult) {

        // 0. Manejo de errores de validación (ej. @NotNull, @Pattern)
        if (bindingResult.hasErrors()) {
            String errores = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(new RespuestaDTO("error", "Validación fallida: " + errores, null));
        }

        // 1. Delegar al servicio
        RespuestaDTO respuesta = pedidoService.procesarPedido(inputDto);
        return ResponseEntity.ok(respuesta);
    }

    /** * POST /api/pedidos/cargar_archivo
     * Agrega pedidos masivamente desde un archivo.
     */
    @PostMapping(value = "/cargar_archivo", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> cargarPedidosArchivo(@RequestParam("archivo") MultipartFile archivo) {
        RespuestaDTO respuesta = pedidoService.cargarPedidosArchivo(archivo);
        // El servicio maneja el bucle y la respuesta de resumen.
        return ResponseEntity.ok(respuesta);
    }

    //Endpoint to delete all pedidos
    @DeleteMapping("/eliminar_todos")
    public ResponseEntity<RespuestaDTO> eliminarTodosLosPedidos() {
        RespuestaDTO respuesta = pedidoService.eliminarTodosLosPedidos();
        return ResponseEntity.ok(respuesta);
    }

    // --------------------------------------------------------------------
    // ENDPOINTS DE CONSULTA (GET)
    // --------------------------------------------------------------------

    /** * GET /api/pedidos/por_cliente/{nombreCliente}
     * Obtiene pedidos por el nombre del cliente (ej: 0000006).
     */
    @GetMapping("/por_cliente/{nombreCliente}")
    public ResponseEntity<RespuestaDTO> obtenerPedidosPorCliente(@PathVariable String nombreCliente) {
        RespuestaDTO respuesta = pedidoService.obtenerPedidosPorCliente(nombreCliente);
        return ResponseEntity.ok(respuesta);
    }

    /** * GET /api/pedidos/por_vuelo_y_fecha
     * Obtiene pedidos que involucran un aeropuerto en un día específico.
     * Ejemplo: GET /api/pedidos/por_vuelo_y_fecha?icao=SKBO&fecha=2025-01-05T00:00:00Z
     */
    @GetMapping("/por_vuelo_y_fecha")
    public ResponseEntity<RespuestaDTO> obtenerPedidosPorVueloYFecha(
            @RequestParam String icao,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fecha) {

        RespuestaDTO respuesta = pedidoService.obtenerPedidosPorVueloYFecha(icao.toUpperCase(), fecha);
        return ResponseEntity.ok(respuesta);
    }
}
