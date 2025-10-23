package com.morapack.controller;

import com.morapack.dto.PlanResponse;
import com.morapack.service.PlanificadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/planificador")
public class PlanificadorController {

    private final PlanificadorService planificadorService;

    public PlanificadorController(PlanificadorService planificadorService) {
        this.planificadorService = planificadorService;
    }

    @PostMapping("/plan")
    public ResponseEntity<List<PlanResponse>> generarPlan(
            @RequestParam String aeropuertosPath,
            @RequestParam String vuelosPath,
            @RequestParam String pedidosPath,
            @RequestParam(required = false) String cancelacionesPath,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            Path aeropTxt = Path.of(aeropuertosPath);
            Path vuelosTxt = Path.of(vuelosPath);
            Path pedidosTxt = Path.of(pedidosPath);
            Path cancelTxt = cancelacionesPath != null ? Path.of(cancelacionesPath) : null;

            YearMonth periodo = YearMonth.of(year, month);

            List<PlanResponse> plan = planificadorService.generateWeeklyPlan(
                    aeropTxt, vuelosTxt, pedidosTxt, cancelTxt, periodo);

            return ResponseEntity.ok(plan);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Collections.emptyList());
        }
    }
}