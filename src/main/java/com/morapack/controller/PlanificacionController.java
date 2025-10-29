package com.morapack.controller;

import com.morapack.dto.OrderPlanDTO;
import com.morapack.dto.PlanificacionSemanalDTO;
import com.morapack.dto.RespuestaDTO;
import com.morapack.service.OrderPlanningService;
import com.morapack.service.PlanificacionService;
import com.morapack.service.SemanalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/planificacion")
@CrossOrigin(origins = "http://localhost:5173")
public class    PlanificacionController {

    @Autowired
    private PlanificacionService planificacionService;

    @Autowired
    private OrderPlanningService orderPlanningService;

    @Autowired
    private SemanalService semanalService;

    @GetMapping("/semanal")
    public ResponseEntity<PlanificacionSemanalDTO> planificarSemanal() {
        // Obtener fecha actual truncada a medianoche UTC
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
        
        // Fecha fin es 7 días después
        Instant fechaInicio = now;
        Instant fechaFin = now.plus(7, ChronoUnit.DAYS);
        
        PlanificacionSemanalDTO resultado = planificacionService.planificarSemanal(fechaInicio, fechaFin);
        return ResponseEntity.ok(resultado);
    }
    
    @GetMapping("/semanal/custom")
    public ResponseEntity<PlanificacionSemanalDTO> planificarSemanalCustom(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaFin) {

        PlanificacionSemanalDTO resultado = planificacionService.planificarSemanal(fechaInicio, fechaFin);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para planificar pedidos con el nuevo formato.
     * Retorna un JSON con orderId, splits y legs.
     */
    @GetMapping("/pedidos")
    public ResponseEntity<List<OrderPlanDTO>> planificarPedidos() {
        List<OrderPlanDTO> resultado = orderPlanningService.planificarPedidos();
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint para procesar cancelaciones de vuelos.
     * Recibe un archivo con el formato: dia-hora-idVuelo-origen-destino
     * Marca los vuelos como cancelados y replanifica los pedidos afectados.
     */
    @PostMapping(value = "/cancelaciones", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> procesarCancelaciones(
            @RequestParam("archivo") MultipartFile archivo) {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(
                new RespuestaDTO("error", "Archivo vacío", null));
        }

        RespuestaDTO respuesta = semanalService.procesarCancelaciones(archivo);
        return ResponseEntity.ok(respuesta);
    }
}