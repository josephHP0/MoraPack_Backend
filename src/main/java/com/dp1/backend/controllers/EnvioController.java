package com.dp1.backend.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.dp1.backend.models.Aeropuerto;
import com.dp1.backend.models.Envio;
import com.dp1.backend.models.Paquete;
import com.dp1.backend.models.Vuelo;
import com.dp1.backend.services.DatosEnMemoriaService;
import com.dp1.backend.services.EnvioService;
import com.dp1.backend.services.PaqueteService;
import com.dp1.backend.utils.ACO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("/envio")
public class EnvioController {
    private static final Logger logger = LogManager.getLogger(EnvioController.class);
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private EnvioService envioService;

    @Autowired
    private PaqueteService paqueteService;

    @Autowired
    private DatosEnMemoriaService datosEnMemoriaService;

    @Autowired
    private ACO aco;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    @GetMapping("/{id}")
    public Envio getEnvio(@PathVariable(name = "id", required = true) int id) {
        return envioService.getEnvio(id);
    }

    @GetMapping("/codigo/{codigo_envio}")
    public Envio getEnvio(@PathVariable(name = "codigo_envio", required = true) String codigo_envio) {
        return envioService.getEnvioCodigo(codigo_envio);
    }

    @GetMapping()
    public ArrayList<Envio> getEnvios() {
        return envioService.getEnvios();
    }

    @PostMapping
    public String createEnvio(@RequestBody Envio envio) {
        logger.info("Creando nuevo envío: " + envio.getOrigen() + " -> " + envio.getDestino());

        // Crear el envío y los paquetes
        String codigosPaquetes = envioService.createEnvio(envio);

        // Asignar rutas a los paquetes usando ACO
        try {
            asignarRutasAPaquetes(envio);
        } catch (Exception e) {
            logger.error("Error al asignar rutas: " + e.getMessage());
            e.printStackTrace();
        }

        return codigosPaquetes;
    }

    private void asignarRutasAPaquetes(Envio envio) {
        try {
            logger.info("Asignando rutas para envío: " + envio.getCodigoEnvio());

            // Obtener aeropuertos y vuelos
            HashMap<String, Aeropuerto> aeropuertos = datosEnMemoriaService.getAeropuertos();
            HashMap<Integer, Vuelo> vuelos = datosEnMemoriaService.getVuelos();

            // Obtener el envío recién creado con sus paquetes
            Envio envioCompleto = envioService.getEnvioCodigo(envio.getOrigen() + envio.getId());
            if (envioCompleto == null) {
                logger.error("No se pudo obtener el envío completo");
                return;
            }

            // Obtener los paquetes del envío
            List<Paquete> paquetesList = paqueteService.getPaquetesByCodigoEnvio(envioCompleto.getCodigoEnvio());
            if (paquetesList == null || paquetesList.isEmpty()) {
                logger.error("No se encontraron paquetes para el envío");
                return;
            }

            ArrayList<Paquete> paquetes = new ArrayList<>(paquetesList);
            logger.info("Paquetes a rutear: " + paquetes.size());

            // Crear un mapa con el envío
            HashMap<String, Envio> envios = new HashMap<>();
            envioCompleto.setPaquetes(paquetes);
            envios.put(envioCompleto.getCodigoEnvio(), envioCompleto);

            // Ejecutar ACO para asignar rutas
            long startTime = System.currentTimeMillis();
            ArrayList<Paquete> paquetesConRutas = aco.run_v2(aeropuertos, vuelos, envios, paquetes, 20);
            long endTime = System.currentTimeMillis();

            logger.info("ACO ejecutado en " + (endTime - startTime) + " ms");
            logger.info("Paquetes con rutas asignadas: " + paquetesConRutas.size());

            // Guardar las rutas asignadas
            for (Paquete paquete : paquetesConRutas) {
                try {
                    // Log detallado de la ruta asignada
                    logger.info("Paquete " + paquete.getId() + " | IdPaquete: " + paquete.getIdPaquete());
                    if (paquete.getRuta() != null && !paquete.getRuta().isEmpty()) {
                        logger.info("  Ruta: " + paquete.getRuta());
                        logger.info("  Fechas: " + paquete.getFechasRuta());
                        logger.info("  Costos: " + paquete.getcostosRuta());
                        logger.info("  LlegóDestino: " + paquete.getLlegoDestino());
                    } else {
                        logger.warn("  ¡¡¡SIN RUTA ASIGNADA!!!");
                    }
                    paqueteService.updatePaquete(paquete);
                } catch (Exception e) {
                    logger.error("Error al actualizar paquete " + paquete.getId() + ": " + e.getMessage());
                }
            }

            logger.info("Rutas asignadas y guardadas exitosamente");

        } catch (Exception e) {
            logger.error("Error en asignarRutasAPaquetes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PutMapping
    public Envio updateEnvio(@RequestBody Envio envio) {
        return envioService.updateEnvio(envio);
    }

    @DeleteMapping("/{id}")
    public String deleteEnvio(@PathVariable(name = "id", required = true) int id) {
        return envioService.deleteEnvio(id);
    }
    
    
}
