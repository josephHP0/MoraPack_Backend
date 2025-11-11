package com.morapack.controller;

import com.morapack.dto.SimulacionEstadoDto;
import com.morapack.dto.SimulacionRequestDto;
import com.morapack.dto.SimulacionResponseDto;
import com.morapack.dto.SimulacionResultadoDto;
import com.morapack.service.SimulacionSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de simulaciones semanales.
 *
 * Endpoints:
 * - POST /api/simulacion/semanal/iniciar - Iniciar nueva simulación
 * - GET /api/simulacion/semanal/estado/{id} - Consultar estado
 * - GET /api/simulacion/semanal/resultados/{id} - Obtener resultados completos
 */
@Slf4j
@RestController
@RequestMapping("/api/simulacion")
@RequiredArgsConstructor
@Tag(name = "Simulación Semanal", description = "APIs para gestión de simulaciones de planificación semanal")
@CrossOrigin(origins = "*", allowCredentials = "false") // Permitir desde cualquier origen (ajustar en producción)
public class SimulacionController {

    private final SimulacionSemanalService simulacionService;

    /**
     * Inicia una nueva simulación semanal.
     *
     * @param request Parámetros de la simulación
     * @return Respuesta con ID de simulación creada
     */
    @PostMapping("/semanal/iniciar")
    @Operation(summary = "Iniciar simulación semanal",
               description = "Crea y ejecuta una nueva simulación de planificación para una ventana temporal")
    public ResponseEntity<SimulacionResponseDto> iniciarSimulacion(
            @Valid @RequestBody SimulacionRequestDto request) {

        log.info("POST /api/simulacion/semanal/iniciar - Iniciando simulación");
        log.debug("Request: {}", request);

        try {
            SimulacionResponseDto response = simulacionService.iniciarSimulacion(request);
            log.info("Simulación creada con ID: {}", response.getSimulacionId());

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Request inválido: {}", e.getMessage());
            SimulacionResponseDto errorResponse = SimulacionResponseDto.builder()
                .estado("ERROR")
                .mensaje("Request inválido: " + e.getMessage())
                .build();
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);

        } catch (Exception e) {
            log.error("Error iniciando simulación: {}", e.getMessage(), e);
            SimulacionResponseDto errorResponse = SimulacionResponseDto.builder()
                .estado("ERROR")
                .mensaje("Error interno: " + e.getMessage())
                .build();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Consulta el estado actual de una simulación.
     *
     * @param id ID de la simulación
     * @return Estado actual de la simulación
     */
    @GetMapping("/semanal/estado/{id}")
    @Operation(summary = "Consultar estado de simulación",
               description = "Obtiene el estado actual y progreso de una simulación")
    public ResponseEntity<SimulacionEstadoDto> obtenerEstado(@PathVariable Integer id) {

        log.info("GET /api/simulacion/semanal/estado/{} - Consultando estado", id);

        try {
            SimulacionEstadoDto estado = simulacionService.obtenerEstado(id);
            return ResponseEntity.ok(estado);

        } catch (RuntimeException e) {
            log.warn("Simulación no encontrada: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error consultando estado de simulación {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Obtiene los resultados completos de una simulación.
     *
     * @param id ID de la simulación
     * @return Resultados completos con asignaciones, métricas y alertas
     */
    @GetMapping("/semanal/resultados/{id}")
    @Operation(summary = "Obtener resultados de simulación",
               description = "Obtiene los resultados completos de una simulación (asignaciones, métricas, alertas)")
    public ResponseEntity<SimulacionResultadoDto> obtenerResultados(@PathVariable Integer id) {

        log.info("GET /api/simulacion/semanal/resultados/{} - Consultando resultados", id);

        try {
            SimulacionResultadoDto resultados = simulacionService.obtenerResultados(id);
            return ResponseEntity.ok(resultados);

        } catch (RuntimeException e) {
            log.warn("Simulación no encontrada: {}", id);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error consultando resultados de simulación {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Endpoint de health check.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica que el servicio esté activo")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Servicio de simulación activo");
    }
}
