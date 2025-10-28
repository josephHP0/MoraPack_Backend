package com.morapack.controller;

import com.morapack.dto.RespuestaDTO;
import com.morapack.service.SemanalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/semanal")
public class SemanalController {

    @Autowired
    private SemanalService semanalService;

    @PostMapping(value = "/cargar", consumes = "multipart/form-data")
    public ResponseEntity<RespuestaDTO> cargarPedidosSemanales(
            @RequestParam("archivo") MultipartFile archivo) {
        
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new RespuestaDTO("error", "Archivo vacío", null));
        }

        RespuestaDTO respuesta = semanalService.procesarPedidosSemanales(archivo);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/planificacion")
    public ResponseEntity<RespuestaDTO> obtenerPlanificacionSemanal() {
        RespuestaDTO respuesta = semanalService.obtenerPlanificacionSemanal();
        return ResponseEntity.ok(respuesta);
    }
}