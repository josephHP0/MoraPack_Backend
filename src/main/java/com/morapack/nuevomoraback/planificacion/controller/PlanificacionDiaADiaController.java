package com.morapack.nuevomoraback.planificacion.controller;

import com.morapack.nuevomoraback.planificacion.dto.EstadoPedidoDTO;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaRequest;
import com.morapack.nuevomoraback.planificacion.dto.PlanificacionDiaADiaResponse;
import com.morapack.nuevomoraback.planificacion.service.PlanificadorDiaADiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dia-a-dia")
@RequiredArgsConstructor
@Tag(name = "Operación Día a Día", description = "Endpoints para planificación continua día a día")
public class PlanificacionDiaADiaController {

    private final PlanificadorDiaADiaService planificadorDiaADiaService;

    @PostMapping("/planificar")
    @Operation(summary = "Planifica ventana de pedidos",
               description = "Planifica pedidos en una ventana corta (1-2 horas) del backlog actual")
    public ResponseEntity<PlanificacionDiaADiaResponse> planificarVentana(
            @Valid @RequestBody PlanificacionDiaADiaRequest request) {

        PlanificacionDiaADiaResponse response = planificadorDiaADiaService.planificarVentana(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado")
    @Operation(summary = "Consulta estado actual",
               description = "Obtiene el estado actual del backlog (pendientes, en tránsito, entregados)")
    public ResponseEntity<List<EstadoPedidoDTO>> consultarEstado() {

        List<EstadoPedidoDTO> estados = planificadorDiaADiaService.consultarEstado();
        return ResponseEntity.ok(estados);
    }
}
