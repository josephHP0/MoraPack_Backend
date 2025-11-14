package com.morapack.nuevomoraback.planificacion.controller;

import com.morapack.nuevomoraback.planificacion.dto.AssignmentByOrderDTO;
import com.morapack.nuevomoraback.planificacion.service.PlanificadorSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para endpoints de planificación (compatibilidad con frontend)
 */
@RestController
@RequestMapping("/api/semanal")
@RequiredArgsConstructor
@Tag(name = "Planificación", description = "Endpoints para consulta de planificación")
public class PlanificacionController {

    private final PlanificadorSemanalService planificadorSemanalService;

    @GetMapping("/planificacion")
    @Operation(summary = "Obtiene planificación de rutas",
               description = "Obtiene todas las asignaciones de rutas planeadas para visualización en el mapa")
    public ResponseEntity<List<AssignmentByOrderDTO>> obtenerPlanificacion() {

        List<AssignmentByOrderDTO> assignments = planificadorSemanalService.obtenerPlanificacion();
        return ResponseEntity.ok(assignments);
    }
}
