package com.morapack.controller;

import com.morapack.dto.RespuestaDTO;
import com.morapack.dto.VueloInputDto;
import com.morapack.service.VueloService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de Vuelos Programados.
 */
@RestController
@RequestMapping("/api/vuelos") // Ruta base CLAVE
@CrossOrigin(origins = "http://localhost:5173")

public class VueloController {

    @Autowired
    private VueloService vueloService;

    // ========================================================================
    // ENDPOINTS DE ALTA (POST)
    // ========================================================================

    /** POST /api/vuelos/formulario */
    @PostMapping("/formulario")
    public ResponseEntity<RespuestaDTO> cargarVueloFormulario(
            @Valid @RequestBody VueloInputDto inputDto,
            BindingResult bindingResult) {

        RespuestaDTO respuesta = vueloService.cargarVueloFormulario(inputDto, bindingResult);
        return ResponseEntity.ok(respuesta);
    }

    /** POST /api/vuelos/archivo */
    @PostMapping(value = "/archivo", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> cargarVuelosArchivo(
            @RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespuestaDTO("error", "Archivo vacío", null));
        }
        RespuestaDTO respuesta = vueloService.cargarVuelosArchivo(archivo);
        return ResponseEntity.ok(respuesta);
    }

    // ========================================================================
    // ENDPOINTS DE CONSULTA (GET)
    // ========================================================================

    /** GET /api/vuelos */
    @GetMapping
    public ResponseEntity<RespuestaDTO> obtenerTodosVuelos() {
        RespuestaDTO respuesta = vueloService.obtenerTodosVuelos();
        return ResponseEntity.ok(respuesta);
    }

    /** GET /api/vuelos */
    @GetMapping("/instances")
    public ResponseEntity<RespuestaDTO> obtenerFlightsDTO() {
        RespuestaDTO respuesta = vueloService.obtenerFlightsDTO();
        return ResponseEntity.ok(respuesta);
    }

// VueloController.java

    @GetMapping("/instances2")
    public ResponseEntity<RespuestaDTO> obtenerFlightsDTO2(
            // Por defecto, comienza en la página 0 y devuelve 1000 vuelos.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        // El servicio ahora recibe los parámetros de paginación
        RespuestaDTO respuesta = vueloService.obtenerFlightsDTO2(page, size);
        return ResponseEntity.ok(respuesta);
    }

    /** GET /api/vuelos/{icaoCodigo} */
    @GetMapping("/{icaoCodigo}")
    public ResponseEntity<RespuestaDTO> obtenerVuelosPorAeropuerto(
            @PathVariable String icaoCodigo) {

        // La validación del ICAO se mantiene en el controlador por ser validación de entrada simple
        if (icaoCodigo == null || icaoCodigo.length() != 4 || !icaoCodigo.matches("^[A-Z]{4}$")) {
            return ResponseEntity.badRequest().body(new RespuestaDTO("error", "El código ICAO debe ser de 4 letras mayúsculas.", null));
        }

        RespuestaDTO respuesta = vueloService.obtenerVuelosPorAeropuerto(icaoCodigo.toUpperCase());
        return ResponseEntity.ok(respuesta);
    }
}