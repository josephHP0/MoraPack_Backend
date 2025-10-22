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
import com.morapack.dto.AeropuertoOutputDto;
import java.util.List;

/**
 * Controlador REST para la gestión de Aeropuertos y Ciudades.
 */
@RestController
@RequestMapping("/api/aeropuertos") // Ruta base CLAVE
@CrossOrigin(origins = "http://localhost:3000")
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

    // ========================================================================
    // ENDPOINTS DE CONSULTA PARA EL FRONT (GET /api/aeropuertos/listar ...)
    // ==========================================================================
    /**
     * GET /api/aeropuertos/listar
     * Devuelve todos los aeropuertos en formato AeropuertoOutputDto (AirportData[] del front)
     */
    @GetMapping("/todos")
    public ResponseEntity<List<AeropuertoOutputDto>> listarAeropuertosParaApi() {
        List<AeropuertoOutputDto> lista = aeropuertoService.listarAeropuertosParaApi();
        return ResponseEntity.ok(lista);
    }


    @GetMapping("/buscar/{icao}")
    public ResponseEntity<AeropuertoOutputDto> obtenerAeropuertoPorIcao(@PathVariable String icao) {
        AeropuertoOutputDto dto = aeropuertoService.obtenerAeropuertoPorIcao(icao);
        return ResponseEntity.ok(dto);
    } 



}