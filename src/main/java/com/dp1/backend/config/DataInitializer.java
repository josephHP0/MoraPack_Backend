package com.dp1.backend.config;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.dp1.backend.models.Aeropuerto;
import com.dp1.backend.models.Envio;
import com.dp1.backend.models.Paquete;
import com.dp1.backend.models.Vuelo;
import com.dp1.backend.services.EnvioService;
import com.dp1.backend.services.DatosEnMemoriaService;
import com.dp1.backend.services.PaqueteService;
import com.dp1.backend.utils.ACO;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(DataInitializer.class);

    @Autowired
    private EnvioService envioService;

    @Autowired
    private DatosEnMemoriaService datosEnMemoriaService;

    @Autowired
    private PaqueteService paqueteService;

    @Autowired
    private ACO aco;

    private Random random = new Random();

    // Códigos OACI de aeropuertos disponibles
    private String[] aeropuertos = {
        "SKBO", // Bogotá
        "SEQM", // Quito
        "SVMI", // Caracas
        "SBBR", // Brasilia
        "SPIM", // Lima
        "SLLP", // La Paz
        "SCEL", // Santiago
        "SABE", // Buenos Aires
        "SGAS", // Asunción
        "SUAA", // Montevideo
        "LATI", // Tirana
        "EDDI", // Berlin
        "LOWW", // Viena
        "EBCI", // Bruselas
        "UMMS", // Minsk
        "LBSF", // Sofia
        "LKPR", // Praga
        "LDZA", // Zagreb
        "EKCH", // Copenhague
        "EHAM", // Amsterdam
        "VIDP", // Delhi
        "OSDI", // Damasco
        "OERK", // Riad
        "OMDB", // Dubai
        "OAKB", // Kabul
        "OOMS", // Mascate
        "OYSN", // Sana
        "OPKC", // Karachi
        "UBBB", // Baku
        "OJAI"  // Aman
    };

    @Override
    public void run(String... args) throws Exception {
        logger.info("========================================");
        logger.info("Iniciando carga de datos hardcodeados de envíos");
        logger.info("========================================");

        try {
            ZonedDateTime tiempoReferencia = crearEnviosHardcodeados();
            logger.info("Envíos creados. Ahora asignando rutas con ACO...");
            asignarRutasConACO(tiempoReferencia);
            logger.info("Datos hardcodeados cargados exitosamente con rutas asignadas");
        } catch (Exception e) {
            logger.error("Error al cargar datos hardcodeados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ZonedDateTime crearEnviosHardcodeados() {
        ZonedDateTime ahora = ZonedDateTime.now();

        // Crear 20 envíos con diferentes características
        logger.info("Creando envíos hardcodeados...");

        // Envíos que salieron hace poco (últimas horas)
        crearEnvio("SPIM", "SKBO", ahora.minusHours(2), 5);  // Lima -> Bogotá
        crearEnvio("SCEL", "SBBR", ahora.minusHours(3), 8);  // Santiago -> Brasilia
        crearEnvio("SABE", "SEQM", ahora.minusHours(1), 3);  // Buenos Aires -> Quito
        crearEnvio("EDDI", "EBCI", ahora.minusHours(4), 6);  // Berlin -> Bruselas
        crearEnvio("VIDP", "OMDB", ahora.minusHours(2), 4);  // Delhi -> Dubai

        // Envíos intercontinentales
        crearEnvio("SPIM", "EDDI", ahora.minusHours(6), 10); // Lima -> Berlin
        crearEnvio("SKBO", "EHAM", ahora.minusHours(8), 12); // Bogotá -> Amsterdam
        crearEnvio("VIDP", "SCEL", ahora.minusHours(10), 7); // Delhi -> Santiago
        crearEnvio("EBCI", "SBBR", ahora.minusHours(5), 9);  // Bruselas -> Brasilia

        // Envíos regionales en América del Sur
        crearEnvio("SPIM", "SCEL", ahora.minusHours(1), 6);  // Lima -> Santiago
        crearEnvio("SKBO", "SPIM", ahora.minusMinutes(30), 4); // Bogotá -> Lima
        crearEnvio("SCEL", "SABE", ahora.minusHours(2), 5);  // Santiago -> Buenos Aires
        crearEnvio("SBBR", "SPIM", ahora.minusHours(3), 7);  // Brasilia -> Lima

        // Envíos regionales en Europa
        crearEnvio("EDDI", "LKPR", ahora.minusMinutes(45), 3); // Berlin -> Praga
        crearEnvio("EBCI", "EHAM", ahora.minusHours(1), 4);  // Bruselas -> Amsterdam
        crearEnvio("LOWW", "LBSF", ahora.minusHours(2), 5);  // Viena -> Sofia

        // Envíos regionales en Asia
        crearEnvio("VIDP", "OPKC", ahora.minusHours(1), 6);  // Delhi -> Karachi
        crearEnvio("OMDB", "OERK", ahora.minusMinutes(90), 4); // Dubai -> Riad
        crearEnvio("UBBB", "OMDB", ahora.minusHours(3), 5);  // Baku -> Dubai

        // Envíos que están por salir (próximas horas)
        crearEnvio("SPIM", "SABE", ahora.plusMinutes(30), 8); // Lima -> Buenos Aires
        crearEnvio("EDDI", "VIDP", ahora.plusHours(1), 10);  // Berlin -> Delhi
        crearEnvio("SKBO", "SCEL", ahora.plusMinutes(45), 6); // Bogotá -> Santiago

        logger.info("22 envíos hardcodeados creados exitosamente");
        return ahora;
    }

    private void crearEnvio(String origen, String destino, ZonedDateTime fechaSalida, int cantidadPaquetes) {
        try {
            Envio envio = new Envio();
            envio.setOrigen(origen);
            envio.setDestino(destino);
            envio.setFechaHoraSalida(fechaSalida);
            envio.setCantidadPaquetes(cantidadPaquetes);

            String resultado = envioService.createEnvio(envio);
            logger.info("Envío creado: " + origen + " -> " + destino +
                       " | Salida: " + fechaSalida +
                       " | Paquetes: " + cantidadPaquetes +
                       " | IDs: " + resultado);
        } catch (Exception e) {
            logger.error("Error al crear envío " + origen + " -> " + destino + ": " + e.getMessage());
        }
    }

    private void asignarRutasConACO(ZonedDateTime tiempoReferencia) {
        try {
            logger.info("Ejecutando ACO para asignar rutas a los paquetes...");

            HashMap<String, Aeropuerto> aeropuertos = datosEnMemoriaService.getAeropuertos();
            HashMap<Integer, Vuelo> vuelos = datosEnMemoriaService.getVuelos();

            // Obtener envíos recientes (últimas 12 horas)
            HashMap<String, Envio> envios = envioService.getEnviosEntrev2(
                tiempoReferencia.minusHours(12),
                tiempoReferencia.plusHours(2)
            );

            logger.info("Envíos a procesar con ACO: " + envios.size());
            logger.info("Aeropuertos: " + aeropuertos.size());
            logger.info("Vuelos: " + vuelos.size());

            // Recolectar todos los paquetes de los envíos
            ArrayList<Paquete> paquetes = new ArrayList<>();
            for (Envio envio : envios.values()) {
                if (envio.getPaquetes() != null) {
                    paquetes.addAll(envio.getPaquetes());
                }
            }

            logger.info("Paquetes a rutear: " + paquetes.size());

            if (paquetes.size() > 0) {
                // Ejecutar ACO para asignar rutas
                long startTime = System.currentTimeMillis();
                ArrayList<Paquete> paquetesConRutas = aco.run_v2(aeropuertos, vuelos, envios, paquetes, 20);
                long endTime = System.currentTimeMillis();

                logger.info("ACO ejecutado en " + (endTime - startTime) + " ms");
                logger.info("Paquetes con rutas asignadas: " + paquetesConRutas.size());

                // Guardar las rutas asignadas
                for (Paquete paquete : paquetesConRutas) {
                    try {
                        paqueteService.updatePaquete(paquete);
                    } catch (Exception e) {
                        logger.error("Error al actualizar paquete: " + e.getMessage());
                    }
                }

                logger.info("Rutas asignadas y guardadas exitosamente");
            } else {
                logger.warn("No se encontraron paquetes para asignar rutas");
            }
        } catch (Exception e) {
            logger.error("Error al asignar rutas con ACO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
