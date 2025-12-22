package com.dp1.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

import com.dp1.backend.models.Envio;
import com.dp1.backend.services.DatosEnMemoriaService;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    @Autowired
    private DatosEnMemoriaService datosEnMemoriaService;

    @PostMapping("/cadena")
    public ResponseEntity<?> registrarCadena(@RequestBody Map<String, String> body) {
        String cadena = body.get("cadena");
        datosEnMemoriaService.insertarCadena(cadena);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/envio/{codigo}")
    public ResponseEntity<?> getEnvioByCodigo(@PathVariable String codigo) {
        Envio envio = datosEnMemoriaService.getEnvios().get(codigo);
        if (envio == null) {
            return ResponseEntity.badRequest().body("No se encontró el envío con código: " + codigo);
        }
        // Solo devolver idEnvio y la cadena relevante
        HashMap<String, Object> result = new HashMap<>();
        result.put("idEnvio", envio.getIdEnvio());
        result.put("codigoEnvio", envio.getCodigoEnvio());
        result.put("origen", envio.getOrigen());
        result.put("destino", envio.getDestino());
        return ResponseEntity.ok(result);
    }
}
