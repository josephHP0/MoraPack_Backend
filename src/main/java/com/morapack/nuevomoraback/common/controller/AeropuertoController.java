package com.morapack.nuevomoraback.common.controller;

import com.morapack.nuevomoraback.common.dto.AeropuertoDTO;
import com.morapack.nuevomoraback.common.service.AeropuertoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aeropuertos")
@RequiredArgsConstructor
@Tag(name = "Aeropuertos", description = "Endpoints para consultar información de aeropuertos")
public class AeropuertoController {

    private final AeropuertoService aeropuertoService;

    @GetMapping
    @Operation(
        summary = "Obtiene lista de aeropuertos",
        description = """
            Retorna la lista de aeropuertos con sus coordenadas geográficas para visualización en mapas.

            Parámetros opcionales:
            - tipo: "hub" para obtener solo hubs (Lima, Bruselas, Bakú)
            - tipo: "no-hub" para obtener solo destinos finales
            - Sin parámetro: retorna todos los aeropuertos

            Cada aeropuerto incluye:
            - Código ICAO (ej: "SPIM", "SPJC")
            - Nombre/alias (ej: "Lima", "Juliaca")
            - Coordenadas (latitud, longitud)
            - Capacidad y zona horaria
            - Flag esHub para distinguir hubs de destinos
            """
    )
    public ResponseEntity<List<AeropuertoDTO>> obtenerAeropuertos(
            @RequestParam(required = false) String tipo) {

        List<AeropuertoDTO> aeropuertos;

        if ("hub".equalsIgnoreCase(tipo)) {
            aeropuertos = aeropuertoService.obtenerAeropuertosHub();
        } else if ("no-hub".equalsIgnoreCase(tipo)) {
            aeropuertos = aeropuertoService.obtenerAeropuertosNoHub();
        } else {
            aeropuertos = aeropuertoService.obtenerTodosLosAeropuertos();
        }

        return ResponseEntity.ok(aeropuertos);
    }

    @GetMapping("/hubs")
    @Operation(
        summary = "Obtiene solo aeropuertos HUB",
        description = "Retorna únicamente los aeropuertos HUB (Lima, Bruselas, Bakú)"
    )
    public ResponseEntity<List<AeropuertoDTO>> obtenerHubs() {
        return ResponseEntity.ok(aeropuertoService.obtenerAeropuertosHub());
    }

    @GetMapping("/destinos")
    @Operation(
        summary = "Obtiene solo aeropuertos destino (NO-HUB)",
        description = "Retorna únicamente los aeropuertos destino (todos excepto Lima, Bruselas, Bakú)"
    )
    public ResponseEntity<List<AeropuertoDTO>> obtenerDestinos() {
        return ResponseEntity.ok(aeropuertoService.obtenerAeropuertosNoHub());
    }
}
