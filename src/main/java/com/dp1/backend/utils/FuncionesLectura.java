package com.dp1.backend.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import com.dp1.backend.models.Aeropuerto;
import com.dp1.backend.models.Envio;
import com.dp1.backend.models.Paquete;
import com.dp1.backend.models.Vuelo;
import com.dp1.backend.repository.EnvioRepository;

import java.time.format.DateTimeFormatter;
import java.time.Duration;
import com.dp1.backend.services.EnvioService;
import com.dp1.backend.services.PaqueteService;

public class FuncionesLectura {

    public static HashMap<String, Aeropuerto> leerAeropuertos(String archivo) {
        System.out.println("Leyendo aeropuertos desde " + archivo);
        HashMap<String, Aeropuerto> aeropuertos = new HashMap<>();
        String currentContinent = "";
        try (BufferedReader br = Files.newBufferedReader(Paths.get(archivo), Charset.forName("UTF-16"))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty() || lineCount <= 2) {
                    continue; // Skip empty lines and headers
                }
                if (line.contains("America del Sur") || line.contains("Europa") || line.contains("Asia")) {
                    currentContinent = line.trim();
                    if (currentContinent.contains("America")) {
                        currentContinent = "America del Sur";
                    }
                    continue; // Skip continent lines
                }
                String[] parts = line.trim().split("\\s{3,}");
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                int number;
                String oaciCode;
                String city;
                String country = "";
                String shortName = "";
                String gmtStr = "";
                String capacityStr = "";
                String latStr = "";
                String lonStr = "";
                try {
                    if (parts.length == 9) {
                        number = Integer.parseInt(parts[0]);
                        oaciCode = parts[1];
                        city = parts[2];
                        country = parts[3];
                        shortName = parts[4];
                        gmtStr = parts[5];
                        capacityStr = parts[6];
                        latStr = parts[7];
                        lonStr = parts[8];
                    } else if (parts.length == 8) {
                        number = Integer.parseInt(parts[0]);
                        oaciCode = parts[1];
                        city = parts[2];
                        // parts[3] contains country and shortName merged
                        String countryAndShort = parts[3];
                        String[] tokens = countryAndShort.split("\\s+");
                        if (tokens.length < 2) {
                            System.err.println("[WARN] Malformed country/shortName in line " + lineCount + ": " + line);
                            continue;
                        }
                        shortName = tokens[tokens.length - 1];
                        country = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 1));
                        gmtStr = parts[4];
                        capacityStr = parts[5];
                        latStr = parts[6];
                        lonStr = parts[7];
                    } else if (parts.length < 7) {
                        System.err.println("[WARN] Skipping malformed line (too few columns, length=" + parts.length + ") at line " + lineCount + ": " + line);
                        continue;
                    } else {
                        // Unexpected length, fallback: try to use last two elements as lat/long
                        System.err.println("[WARN] Unexpected column count (length=" + parts.length + ") at line " + lineCount + ": " + line);
                        number = Integer.parseInt(parts[0]);
                        oaciCode = parts[1];
                        city = parts[2];
                        country = parts[3];
                        shortName = (parts.length > 4) ? parts[4] : "";
                        gmtStr = (parts.length > 5) ? parts[5] : "";
                        capacityStr = (parts.length > 6) ? parts[6] : "";
                        latStr = (parts.length > 7) ? parts[parts.length - 2] : "";
                        lonStr = (parts.length > 8) ? parts[parts.length - 1] : "";
                    }
                    int gmt = gmtStr.isEmpty() ? 0 : Integer.parseInt(gmtStr.replace("+", ""));
                    int capacidadInfinita = Integer.MAX_VALUE / 2; // suficientemente grande
                    int capacity;
                    if (oaciCode.equals("SPIM") || oaciCode.equals("UBBB") || oaciCode.equals("EBCI")) {
                        capacity = capacidadInfinita;
                    } else {
                        // Intenta parsear capacityStr, si falla usa 850 como valor por defecto
                        try {
                            capacity = Integer.parseInt(capacityStr);
                        } catch (Exception e) {
                            capacity = 850;
                        }
                    }
                    double latitud = latStr.isEmpty() ? 0.0 : convertToDecimalDegrees(latStr);
                    double longitud = lonStr.isEmpty() ? 0.0 : convertToDecimalDegrees(lonStr);

                    Aeropuerto aeropuerto = new Aeropuerto(number, oaciCode, city, country, shortName, gmt, capacity);
                    aeropuerto.setLatitud(latitud);
                    aeropuerto.setLongitud(longitud);
                    aeropuerto.setContinente(currentContinent);
                    aeropuertos.put(oaciCode, aeropuerto);
                } catch (Exception ex) {
                    System.err.println("[ERROR] Failed to parse line " + lineCount + ": " + line);
                    ex.printStackTrace();
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e);
        }
        return aeropuertos;
    }

    public static HashMap<Integer, Vuelo> leerVuelos(String archivo, HashMap<String, Aeropuerto> aeropuertos) {
        System.out.println("Leyendo vuelos desde " + archivo);
        HashMap<Integer, Vuelo> vuelos = new HashMap<>();
        int id = 1;
        try (BufferedReader br = Files.newBufferedReader(Paths.get(archivo), Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] parts = line.split("-");
                String ciudadOrigen = parts[0];
                String ciudadDestino = parts[1];
                String horaOrigen = parts[2];
                String horaDestino = parts[3];

                ZoneId zonaOrigen = aeropuertos.get(ciudadOrigen).getZoneId();
                ZoneId zonaDestino = aeropuertos.get(ciudadDestino).getZoneId();
                // TimeZone zonaOrigen = aeropuertos.get(ciudadOrigen).getZonaHoraria();
                // TimeZone zonaDestino = aeropuertos.get(ciudadDestino).getZonaHoraria();

                LocalDate localDate = LocalDate.now();
                LocalTime origenLocalTime = LocalTime.parse(horaOrigen);
                LocalTime destinoLocalTime = LocalTime.parse(horaDestino);

                ZonedDateTime horaOrigenZoned = ZonedDateTime.of(localDate, origenLocalTime, zonaOrigen);
                ZonedDateTime horaDestinoZoned = ZonedDateTime.of(localDate, destinoLocalTime, zonaDestino);

                // int capacidadCarga = Integer.parseInt(parts[4]) -250 + 100 -70;
                int capacidadCarga = 120;
                double distancia = Auxiliares.calculateHaversineDistance(aeropuertos.get(ciudadOrigen),
                        aeropuertos.get(ciudadDestino));

                Vuelo vuelo = new Vuelo(ciudadOrigen, ciudadDestino, horaOrigenZoned, horaDestinoZoned, capacidadCarga,
                        distancia);
                vuelo.setIdVuelo(id);
                vuelos.put(id, vuelo);
                id++;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e);
        }
        return vuelos;
    }

    private static double convertToDecimalDegrees(String dms) {
        // Remove more than one space
        dms = dms.replaceAll("\\s+", " ");
        String[] parts = dms.split("\\s");
        // Remove °, ', "
        parts[1] = parts[1].replace("°", "");
        parts[2] = parts[2].replace("'", "");
        parts[3] = parts[3].replace("\"", "");

        double degrees = Double.parseDouble(parts[1]);
        double minutes = Double.parseDouble(parts[2]) / 60;
        double seconds = Double.parseDouble(parts[3]) / 3600;
        double decimalDegrees = degrees + minutes + seconds;
        if (parts[4].equals("S") || parts[4].equals("W")) {
            decimalDegrees = -decimalDegrees; // Invert the sign for south and west coordinates
        }
        return decimalDegrees;
    }

    public static HashMap<String, Envio> leerEnvios(String archivo, HashMap<String, Aeropuerto> aeropuertos,
            int maxEnvios) {
        System.out.println("Leyendo envios desde " + archivo);
        HashMap<String, Envio> envios = new HashMap<>();
        int counter = 0;
        try (BufferedReader br = Files.newBufferedReader(Paths.get(archivo), Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null && counter < maxEnvios) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] parts = line.split("-");
                String ciudadOrigenEnvio = parts[0];
                int envioId = Integer.parseInt(parts[1]);
                LocalDate fechaOrigen = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalTime horaOrigen = LocalTime.parse(parts[3]);
                String[] destinoParts = parts[4].split(":");
                String ciudadDestino = destinoParts[0];
                // if(!ciudadDestino.equals("VIDP") && !ciudadDestino.equals("SVMI") &&
                // !ciudadDestino.equals("VTBS")) continue;
                int cantidadPaquetes = Integer.parseInt(destinoParts[1]);

                Aeropuerto origen = aeropuertos.getOrDefault(ciudadOrigenEnvio, aeropuertos.get("EKCH"));
                Aeropuerto destino = aeropuertos.getOrDefault(ciudadDestino, aeropuertos.get("EKCH"));

                ZoneId zonaOrigen = origen.getZoneId();
                ZoneId zonaDestino = destino.getZoneId();

                ZonedDateTime horaOrigenZoned = ZonedDateTime.of(fechaOrigen, horaOrigen, zonaOrigen);
                ZonedDateTime horaDestinoZoned;

                // El tiempo para enviar será de dos días si es continente distsinto y de un día
                // si es el mismo continente
                if (!origen.getContinente().equals(destino.getContinente())) {
                    horaDestinoZoned = horaOrigenZoned.plusDays(2).withZoneSameInstant(zonaDestino);
                } else {
                    horaDestinoZoned = horaOrigenZoned.plusDays(1).withZoneSameInstant(zonaDestino);
                }
                ArrayList<Paquete> paquetes = new ArrayList<>();
                for (int i = 0; i < cantidadPaquetes; i++) {
                    Paquete paquete = new Paquete();
                    paquete.setCodigoEnvio(ciudadOrigenEnvio + envioId);
                    paquete.setIdPaquete(1000000 * origen.getIdAeropuerto() + 100 * envioId + (i + 1));// un envió no
                                                                                                       // tiene más de
                                                                                                       // 99 paquetes en
                                                                                                       // principio
                    // Add more properties to the package if needed
                    if (!origen.getContinente().equals(destino.getContinente())) {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(2));
                        paquete.setTiempoRestante(Duration.ofDays(2));
                    } else {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(1));
                        paquete.setTiempoRestante(Duration.ofDays(1));
                    }

                    paquetes.add(paquete);

                    // Meter paquetes al aeropuerto de origen
                    // origen.paqueteEntraReal(horaOrigenZoned.toLocalDateTime());
                }
                Envio nuevoEnvio = new Envio(ciudadOrigenEnvio, ciudadDestino, horaOrigenZoned, cantidadPaquetes,
                        paquetes);
                nuevoEnvio.setIdEnvio(envioId);
                nuevoEnvio.setFechaHoraLlegadaPrevista(horaDestinoZoned);

                String codigo = ciudadOrigenEnvio + envioId;
                nuevoEnvio.setCodigoEnvio(codigo);
                envios.put(codigo, nuevoEnvio);
                counter++;
            }
            // System.out.println("Numero de envios: " + counter);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e);
        }
        // for(int id: envios.keySet()){
        // System.out.println(envios.get(id).getIdEnvio());
        // }
        return envios;
    }

    public static HashMap<String, Envio> leerEnviosDesdeHasta(String archivo, HashMap<String, Aeropuerto> aeropuertos,
            ZonedDateTime fechaInicio, ZonedDateTime fechaFin) {
        System.out.println("Leyendo envios desde " + archivo);
        HashMap<String, Envio> envios = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(archivo), Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] parts = line.split("-");
                String ciudadOrigenEnvio = parts[0];
                int envioId = Integer.parseInt(parts[1]);
                LocalDate fechaOrigen = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalTime horaOrigen = LocalTime.parse(parts[3]);
                String[] destinoParts = parts[4].split(":");
                String ciudadDestino = destinoParts[0];
                int cantidadPaquetes = Integer.parseInt(destinoParts[1]);

                Aeropuerto origen = aeropuertos.getOrDefault(ciudadOrigenEnvio, aeropuertos.get("EKCH"));
                Aeropuerto destino = aeropuertos.getOrDefault(ciudadDestino, aeropuertos.get("EKCH"));

                ZoneId zonaOrigen = origen.getZoneId();
                ZoneId zonaDestino = destino.getZoneId();

                ZonedDateTime horaOrigenZoned = ZonedDateTime.of(fechaOrigen, horaOrigen, zonaOrigen);
                if (horaOrigenZoned.isBefore(fechaInicio)) {
                    continue;
                }
                if (horaOrigenZoned.isAfter(fechaFin)) {
                    break;
                }
                ZonedDateTime horaDestinoZoned;

                // El tiempo para enviar será de dos días si es continente distsinto y de un día
                // si es el mismo continente
                if (!origen.getContinente().equals(destino.getContinente())) {
                    horaDestinoZoned = horaOrigenZoned.plusDays(2).withZoneSameInstant(zonaDestino);
                } else {
                    horaDestinoZoned = horaOrigenZoned.plusDays(1).withZoneSameInstant(zonaDestino);
                }
                ArrayList<Paquete> paquetes = new ArrayList<>();
                for (int i = 0; i < cantidadPaquetes; i++) {
                    Paquete paquete = new Paquete();
                    paquete.setCodigoEnvio(ciudadOrigenEnvio + envioId);
                    paquete.setIdPaquete(1000000 * origen.getIdAeropuerto() + 100 * envioId + (i + 1));// un envió no
                                                                                                       // tiene más de
                                                                                                       // 99 paquetes en
                                                                                                       // principio
                    // Add more properties to the package if needed
                    if (!origen.getContinente().equals(destino.getContinente())) {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(2));
                        paquete.setTiempoRestante(Duration.ofDays(2));
                    } else {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(1));
                        paquete.setTiempoRestante(Duration.ofDays(1));
                    }

                    paquetes.add(paquete);

                    // Meter paquetes al aeropuerto de origen
                    // origen.paqueteEntraReal(horaOrigenZoned.toLocalDateTime());
                }
                Envio nuevoEnvio = new Envio(ciudadOrigenEnvio, ciudadDestino, horaOrigenZoned, cantidadPaquetes,
                        paquetes);
                nuevoEnvio.setIdEnvio(envioId);
                nuevoEnvio.setFechaHoraLlegadaPrevista(horaDestinoZoned);

                String codigo = ciudadOrigenEnvio + envioId;
                nuevoEnvio.setCodigoEnvio(codigo);
                envios.put(codigo, nuevoEnvio);
            }
            // System.out.println("Numero de envios: " + counter);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e);
        }
        // for(int id: envios.keySet()){
        // System.out.println(envios.get(id).getIdEnvio());
        // }
        return envios;
    }

    public static String leerEnviosGuardarBD(String archivo, HashMap<String, Aeropuerto> aeropuertos, int maxEnvios,
            EnvioRepository envioRepository, PaqueteService paqueteService) throws IOException {
        System.out.println("Leyendo envios desde " + archivo);
        HashMap<String, Envio> envios = new HashMap<>();
        int counter = 0;
        String codigosPaquete = "";
        try (BufferedReader br = Files.newBufferedReader(Paths.get(archivo), Charset.forName("UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null && counter < maxEnvios) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] parts = line.split("-");
                String ciudadOrigenEnvio = parts[0];
                int envioId = Integer.parseInt(parts[1]);
                LocalDate fechaOrigen = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("yyyyMMdd"));
                LocalTime horaOrigen = LocalTime.parse(parts[3]);
                String[] destinoParts = parts[4].split(":");
                String ciudadDestino = destinoParts[0];
                // if(!ciudadDestino.equals("VIDP") && !ciudadDestino.equals("SVMI") &&
                // !ciudadDestino.equals("VTBS")) continue;
                int cantidadPaquetes = Integer.parseInt(destinoParts[1]);

                Aeropuerto origen = aeropuertos.getOrDefault(ciudadOrigenEnvio, aeropuertos.get("EKCH"));
                Aeropuerto destino = aeropuertos.getOrDefault(ciudadDestino, aeropuertos.get("EKCH"));

                ZoneId zonaOrigen = origen.getZoneId();
                ZoneId zonaDestino = destino.getZoneId();

                // System.out.println("fechaOrigen: " + fechaOrigen);
                // System.out.println("horaOrigen: " + horaOrigen);
                // System.out.println("zonaOrigen: " + zonaOrigen);
                ZonedDateTime horaOrigenZoned = ZonedDateTime.of(fechaOrigen, horaOrigen, zonaOrigen);
                ZonedDateTime horaDestinoZoned;

                // System.out.println("horaOrigenZoned: " + horaOrigenZoned);

                // El tiempo para enviar será de dos días si es continente distsinto y de un día
                // si es el mismo continente
                if (!origen.getContinente().equals(destino.getContinente())) {
                    horaDestinoZoned = horaOrigenZoned.plusDays(2).withZoneSameInstant(zonaDestino);
                } else {
                    horaDestinoZoned = horaOrigenZoned.plusDays(1).withZoneSameInstant(zonaDestino);
                }
                ArrayList<Paquete> paquetes = new ArrayList<>();
                for (int i = 0; i < cantidadPaquetes; i++) {
                    Paquete paquete = new Paquete();
                    paquete.setCodigoEnvio(ciudadOrigenEnvio + envioId);
                    paquete.setIdPaquete(1000000 * origen.getIdAeropuerto() + 100 * envioId + (i + 1));// un envió no
                                                                                                       // tiene más de
                                                                                                       // 99 paquetes en
                                                                                                       // principio
                    // Add more properties to the package if needed
                    if (!origen.getContinente().equals(destino.getContinente())) {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(2));
                        paquete.setTiempoRestante(Duration.ofDays(2));
                    } else {
                        paquete.setTiempoRestanteDinamico(Duration.ofDays(1));
                        paquete.setTiempoRestante(Duration.ofDays(1));
                    }

                    paquetes.add(paquete);

                    // Meter paquetes al aeropuerto de origen
                    // origen.paqueteEntraReal(horaOrigenZoned.toLocalDateTime());
                }
                //Ajustar hora origen
                // horaOrigenZoned = horaOrigenZoned.plusHours((-5)-origen.getGmt());

                System.out.println("horaOrigenZoned luego de ajuste: " + horaOrigenZoned);

                Envio nuevoEnvio = new Envio(ciudadOrigenEnvio, ciudadDestino, horaOrigenZoned, cantidadPaquetes,
                        paquetes);
                nuevoEnvio.setIdEnvio(envioId);
                nuevoEnvio.setFechaHoraLlegadaPrevista(horaDestinoZoned);

                String codigo = ciudadOrigenEnvio + envioId;
                
                nuevoEnvio.setEmisorID(23);
                nuevoEnvio.setReceptorID(23);
                envios.put(codigo, nuevoEnvio);
                

                //Revisar si el envío ya existe
                Envio envioExistente = envioRepository.findByCodigoEnvio(codigo);
                if(envioExistente != null){
                    continue;
                }
                nuevoEnvio.setId(0);
                nuevoEnvio.setCodigoEnvio(null);
                nuevoEnvio.setPaquetes(null);
                nuevoEnvio.setEmisor(null);
                nuevoEnvio.setReceptor(null);
                envioRepository.save(nuevoEnvio);
                nuevoEnvio.setCodigoEnvio(nuevoEnvio.getOrigen() + nuevoEnvio.getId());
                envioRepository.save(nuevoEnvio);

                // Por cada paquete, establecer la relación con el envío y guardar en base de
                // datos
                for (Paquete paquete : paquetes) {
                    paquete.setCodigoEnvio(nuevoEnvio.getCodigoEnvio());
                    paquete.setRutaPosible(null);
                    Paquete nuevoPaquete= paqueteService.createPaquete(paquete);
                    codigosPaquete += nuevoPaquete.getIdPaquete() + " ";
                }
                counter++;
            }
            // System.out.println("Numero de envios: " + counter);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e);
            throw e;
        }
        // for(int id: envios.keySet()){
        // System.out.println(envios.get(id).getIdEnvio());
        // }
        return codigosPaquete;
    }

}
