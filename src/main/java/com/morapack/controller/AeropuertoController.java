package com.morapack.controller;

import com.morapack.dto.AeropuertoInputDto;
import com.morapack.dto.RespuestaDTO;
import com.morapack.service.AeropuertoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para la gestión de Aeropuertos y Ciudades.
 */
@RestController
@RequestMapping("/api/aeropuertos") // Ruta base CLAVE
@CrossOrigin(origins = "http://localhost:5173")
public class AeropuertoController {

    @Autowired
    private AeropuertoService aeropuertoService;

    // ========================================================================
    // ENDPOINTS DE ALTA (POST)
    // ========================================================================

    /** POST /api/aeropuertos/formulario */
    @PostMapping("/formulario")
    public ResponseEntity<RespuestaDTO> cargarAeropuertoFormulario(
            @Valid @RequestBody AeropuertoInputDto inputDto,
            BindingResult bindingResult) {
        RespuestaDTO respuesta = aeropuertoService.cargarAeropuertoFormulario(inputDto, bindingResult);
        return ResponseEntity.ok(respuesta);
    }

    /** POST /api/aeropuertos/archivo */
    @PostMapping(value = "/archivo", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> cargarAeropuertosArchivo(
            @RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespuestaDTO("error", "Archivo vacío", null));
        }
        RespuestaDTO respuesta = aeropuertoService.cargarAeropuertosArchivo(archivo);
        return ResponseEntity.ok(respuesta);
    }

    // ========================================================================
    // ENDPOINTS DE CONSULTA (GET)
    // ========================================================================

    /** GET /api/aeropuertos */
    @GetMapping
    public ResponseEntity<RespuestaDTO> obtenerTodosAeropuertos() {
        RespuestaDTO respuesta = aeropuertoService.obtenerTodosAeropuertos();
        return ResponseEntity.ok(respuesta);
    }
}