package com.morapack.nuevomoraback.planificacion.controller;

import com.morapack.nuevomoraback.common.repository.PedidoRepository;
import com.morapack.nuevomoraback.planificacion.dto.DebugPedidosDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionDetalladaDTO;
import com.morapack.nuevomoraback.planificacion.dto.SimulacionSemanalRequest;
import com.morapack.nuevomoraback.planificacion.service.PlanificadorSemanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/simulacion-semanal")
@RequiredArgsConstructor
@Tag(name = "Simulación Semanal", description = "Endpoints para simulación semanal batch con información detallada")
public class PlanificacionSemanalController {

    private final PlanificadorSemanalService planificadorSemanalService;
    private final PedidoRepository pedidoRepository;

    @PostMapping("/ejecutar")
    @Operation(summary = "Ejecuta simulación semanal",
               description = "Planifica pedidos en el rango de fechas. " +
                             "Si duracionBloqueHoras es null, ejecuta todo de golpe (batch). " +
                             "Si tiene valor, divide en bloques del tamaño especificado. " +
                             "Retorna información detallada de vuelos, pedidos y rutas para el frontend.")
    public ResponseEntity<SimulacionDetalladaDTO> ejecutarSimulacion(
            @Valid @RequestBody SimulacionSemanalRequest request) {

        SimulacionDetalladaDTO response = planificadorSemanalService.ejecutarSimulacionSemanal(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resultado/{id}")
    @Operation(summary = "Consulta resultado de simulación con detalle completo",
               description = "Obtiene el resultado con información detallada de vuelos, pedidos asignados y rutas completas")
    public ResponseEntity<SimulacionDetalladaDTO> consultarResultado(@PathVariable Integer id) {

        SimulacionDetalladaDTO response = planificadorSemanalService.consultarResultado(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/pedidos")
    @Operation(summary = "DEBUG - Verifica pedidos disponibles en un rango",
               description = "Endpoint de debug para verificar cuántos pedidos hay en la BD en un rango de fechas. " +
                             "Útil para diagnosticar por qué una simulación retorna 0 pedidos.")
    public ResponseEntity<DebugPedidosDTO> debugPedidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaFin) {

        DebugPedidosDTO debug = planificadorSemanalService.debugPedidosDisponibles(fechaInicio, fechaFin);
        return ResponseEntity.ok(debug);
    }
}
