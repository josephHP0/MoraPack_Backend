package com.morapack.controller;

import com.morapack.dto.AeropuertoDTO;
import com.morapack.dto.RespuestaDTO;
import com.morapack.service.DataService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controlador REST para gestión de aeropuertos (T01Aeropuerto).
 * Endpoints:
 * - POST /api/data/aeropuertos/formulario: Carga un aeropuerto vía JSON.
 * - POST /api/data/aeropuertos/archivo: Carga múltiples aeropuertos vía archivo TXT/CSV.
 * - GET /api/data/aeropuertos: Lista todos los aeropuertos.
 */
@RestController
@RequestMapping("/api/data/aeropuertos")
@CrossOrigin(origins = "http://localhost:3000")  // CORS para React; mueve a config global si prefieres
public class DataController {

    @Autowired
    private DataService dataService;

    /**
     * POST /api/data/aeropuertos/formulario
     * Carga un aeropuerto desde formulario (JSON único).
     * Usa @Valid para validaciones automáticas en AeropuertoDTO.
     */
    @PostMapping("/formulario")
    public ResponseEntity<RespuestaDTO> cargarAeropuertoFormulario(
            @Valid @RequestBody AeropuertoDTO dto,
            BindingResult bindingResult) {
        // Si hay errores de validación, el BindingResult los captura
        RespuestaDTO respuesta = dataService.cargarAeropuertoFormulario(dto, bindingResult);
        return ResponseEntity.ok(respuesta);  // Siempre 200; status "error" en el body si falla
    }

    /**
     * POST /api/data/aeropuertos/archivo
     * Carga múltiples aeropuertos desde archivo subido (multipart/form-data).
     * Campo del form: "archivo" (MultipartFile, extensión .txt o .csv).
     */
    @PostMapping(value = "/archivo", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> cargarAeropuertosArchivo(
            @RequestParam("archivo") MultipartFile archivo) {
        // Validar archivo no vacío
        if (archivo.isEmpty()) {
            RespuestaDTO error = new RespuestaDTO("error", "Archivo vacío o no proporcionado", null);
            return ResponseEntity.badRequest().body(error);
        }

        // Validar extensión (opcional, pero recomendado)
        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo != null && !nombreArchivo.toLowerCase().endsWith(".txt") &&
                !nombreArchivo.toLowerCase().endsWith(".csv")) {
            RespuestaDTO error = new RespuestaDTO("error", "Archivo debe ser .txt o .csv", null);
            return ResponseEntity.badRequest().body(error);
        }

        RespuestaDTO respuesta = dataService.cargarAeropuertosArchivo(archivo);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * GET /api/data/aeropuertos
     * Obtiene todos los aeropuertos cargados en BD.
     * Respuesta: Lista de AeropuertoDTO.
     */
    @GetMapping
    public ResponseEntity<RespuestaDTO> obtenerTodosAeropuertos() {
        RespuestaDTO respuesta = dataService.obtenerTodosAeropuertos();
        return ResponseEntity.ok(respuesta);
    }

    // Opcional: DELETE /api/data/aeropuertos/{id} para eliminar (implementa en Service si necesitas)
    // @DeleteMapping("/{id}")
    // public ResponseEntity<RespuestaDTO> eliminarAeropuerto(@PathVariable String id) { ... }
}
