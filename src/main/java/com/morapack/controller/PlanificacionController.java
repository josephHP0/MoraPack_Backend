package com.morapack.controller;

import com.morapack.dto.PlanificacionSemanalDTO;
import com.morapack.service.PlanificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/planificacion")
public class PlanificacionController {

    @Autowired
    private PlanificacionService planificacionService;

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
}