package com.morapack.nuevomoraback.planificacion.controller;

import com.morapack.nuevomoraback.planificacion.dto.AssignmentByOrderDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalResponse;
import com.morapack.nuevomoraback.planificacion.service.PlanificadorSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulacion-semanal")
@RequiredArgsConstructor
@Tag(name = "Simulación Semanal", description = "Endpoints para simulación semanal batch")
public class PlanificacionSemanalController {

    private final PlanificadorSemanalService planificadorSemanalService;

    @PostMapping("/ejecutar")
    @Operation(summary = "Ejecuta simulación semanal",
               description = "Planifica todos los pedidos en el rango de fechas (semana completa o bloques parametrizables)")
    public ResponseEntity<SimulacionSemanalResponse> ejecutarSimulacion(
            @Valid @RequestBody SimulacionSemanalRequest request) {

        SimulacionSemanalResponse response = planificadorSemanalService.ejecutarSimulacionSemanal(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resultado/{id}")
    @Operation(summary = "Consulta resultado de simulación",
               description = "Obtiene el resultado y métricas de una simulación por su ID")
    public ResponseEntity<SimulacionSemanalResponse> consultarResultado(@PathVariable Integer id) {

        SimulacionSemanalResponse response = planificadorSemanalService.consultarResultado(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/planificacion")
    @Operation(summary = "Obtiene planificación de rutas",
               description = "Obtiene todas las asignaciones de rutas planeadas para visualización en el mapa")
    public ResponseEntity<List<AssignmentByOrderDTO>> obtenerPlanificacion() {

        List<AssignmentByOrderDTO> assignments = planificadorSemanalService.obtenerPlanificacion();
        return ResponseEntity.ok(assignments);
    }
}
