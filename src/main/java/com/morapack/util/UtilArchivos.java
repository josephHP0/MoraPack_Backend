package com.morapack.util;

import com.morapack.dto.AeropuertoDTO;
import com.morapack.dto.VueloDTO;
import com.morapack.dto.PedidoDTO;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades para parsear archivos TXT/CSV de datos de MoraPack.
 * Retorna listas de DTOs para aeropuertos, vuelos y pedidos.
 * Maneja errores por línea sin detener el proceso.
 */
public class UtilArchivos {

    /**
     * DTO para errores de parseo (para retornar detalles al frontend).
     */
    @Data
    public static class ErrorParseo {
        private int lineaNum;
        private String mensaje;
        private String lineaOriginal;

        public ErrorParseo(int lineaNum, String mensaje, String lineaOriginal) {
            this.lineaNum = lineaNum;
            this.mensaje = mensaje;
            this.lineaOriginal = lineaOriginal;
        }
    }

    /**
     * Parser DMS a BigDecimal decimal (ej. "04°42'05\"N" → 4.701389).
     * Soporta N/S para lat, E/W para lon.
     */
    public static BigDecimal parseDmsToDecimal(String dmsStr, boolean isLat) {
        if (dmsStr == null || dmsStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Limpia: remueve espacios y extras no esenciales, pero preserva ° ' " NSEW
            String cleanDms = dmsStr.replaceAll("\\s+", "");  // Remueve todos los espacios
            // Regex: Captura \d+°\d+'\d+"[NSEW] (ej. 35°59'36"E)
            Pattern pattern = Pattern.compile("(\\d+)°(\\d+)['](\\d+)[\"](\\p{Lu})");  // \p{Lu} para N/S/E/W upper
            Matcher matcher = pattern.matcher(cleanDms);

            if (!matcher.matches()) {
                throw new IllegalArgumentException("Formato DMS inválido: " + dmsStr + " (esperado: dd°mm'ss\"[NSEW])");
            }

            BigDecimal grados = new BigDecimal(matcher.group(1));
            BigDecimal minutos = new BigDecimal(matcher.group(2)).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
            BigDecimal segundos = new BigDecimal(matcher.group(3)).divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
            String direccion = matcher.group(4).toUpperCase();

            BigDecimal decimal = grados.add(minutos).add(segundos);

            // Negativo para S (lat) o W (lon)
            if ((isLat && "S".equals(direccion)) || (!isLat && "W".equals(direccion))) {
                decimal = decimal.negate();
            }

            return decimal.setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            System.err.println("Error parseando DMS '" + dmsStr + "': " + e.getMessage());
            return BigDecimal.ZERO;  // Valor default; en producción, lanza excepción
        }
    }

    /**
     * Carga aeropuertos desde archivo (formato: id,codigoIATA,ciudad,pais,alias,gmt,capacidad,latDms,lonDms,continente).
     * @param path Ruta al archivo.
     * @return Map<codigoIATA, AeropuertoDTO> con DTOs válidos.
     */
    public static Map<String, AeropuertoDTO> cargarAeropuertos(Path path) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            return parsearAeropuertos(br, path.toString());
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo: " + e.getMessage(), e);
        }
    }

    public static Map<String, AeropuertoDTO> cargarAeropuertos(MultipartFile archivo) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            return parsearAeropuertos(br, archivo.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivo: " + e.getMessage(), e);
        }
    }

    private static Map<String, AeropuertoDTO> parsearAeropuertos(BufferedReader br, String fuente) {
        Map<String, AeropuertoDTO> aeropuertos = new HashMap<>();
        List<ErrorParseo> errores = new ArrayList<>();
        String line;
        int lineaNum = 0;

        try {
            while ((line = br.readLine()) != null) {
                lineaNum++;
                String lineaLimpia = safeLine(line);
                if (lineaLimpia == null) continue;

                String[] parts = lineaLimpia.split(",", -1);
                if (parts.length != 10) {
                    errores.add(new ErrorParseo(lineaNum, "Se esperan 10 campos", line));
                    continue;
                }
                try {
                    // *** CORRECCIÓN CLAVE: Manejo del ID como Integer y nulo si está vacío. ***
                    String idStr = parts[0].trim();
                    Integer id = null;
                    if (!idStr.isEmpty()) {
                        try {
                            id = Integer.valueOf(idStr); // Intenta parsear el ID si existe
                        } catch (NumberFormatException e) {
                            errores.add(new ErrorParseo(lineaNum, "ID de Aeropuerto inválido (esperado INT o vacío): " + idStr, line));
                            continue;
                        }
                    }
                    String codigoICAO = parts[1].trim();  // t01Codigoiata
                    String ciudadNombre = parts[2].trim();  // t01Ciudadnombre
                    String pais = parts[3].trim();  // t01Pais
                    String alias = parts[4].trim();  // t01Alias
                    Byte gmtOffset = Byte.valueOf(parts[5].trim());  // t01GmtOffset
                    Short capacidad = Short.valueOf(parts[6].trim());  // t01Capacidad
                    String latDms = parts[7].trim();  // DMS crudo: "04°42'05\"N"
                    String lonDms = parts[8].trim();  // DMS crudo: "74°08'49\"W"
                    String continente = parts[9].trim().toUpperCase();

                    // Validaciones básicas
                    if (!codigoICAO.matches("[A-Z]{4}")) {
                        errores.add(new ErrorParseo(lineaNum, "IACO inválido: " + codigoICAO, line));
                        continue;
                    }
                    if (!List.of("AM", "AS", "EU").contains(continente)) {
                        errores.add(new ErrorParseo(lineaNum, "Continente inválido: " + continente, line));
                        continue;
                    }
                    if (aeropuertos.containsKey(codigoICAO)) {
                        errores.add(new ErrorParseo(lineaNum, "IACO duplicado: " + codigoICAO, line));
                        continue;
                    }

                    // Parsear DMS a decimal DESPUÉS de setear las cadenas
                    BigDecimal lat = parseDmsToDecimal(latDms, true);  // Lat (isLat=true)
                    BigDecimal lon = parseDmsToDecimal(lonDms, false);  // Lon (isLat=false)

                    // Si parseo falla (devuelve ZERO), agregar error
                    if (BigDecimal.ZERO.equals(lat) && !latDms.isEmpty()) {
                        errores.add(new ErrorParseo(lineaNum, "Error parseando latDms: " + latDms, line));
                        continue;
                    }
                    if (BigDecimal.ZERO.equals(lon) && !lonDms.isEmpty()) {
                        errores.add(new ErrorParseo(lineaNum, "Error parseando lonDms: " + lonDms, line));
                        continue;
                    }

                    // idCiudad placeholder (integra con T08Ciudad repo si tienes)
                    String idCiudad = "CIUDAD_" + ciudadNombre.toUpperCase().replace(" ", "_");  // Ej. "CIUDAD_BOGOTA"

                    AeropuertoDTO dto = new AeropuertoDTO();
                    dto.setIdAeropuerto(id);
                    dto.setIdCiudad(idCiudad);
                    dto.setCodigoICAO(codigoICAO);
                    dto.setCiudadNombre(ciudadNombre);
                    dto.setPais(pais);
                    dto.setAlias(alias);
                    dto.setContinente(continente);
                    dto.setGmtOffset(gmtOffset);
                    dto.setCapacidad(capacidad);
                    dto.setLatDms(latDms);  // Cadena cruda
                    dto.setLonDms(lonDms);  // Cadena cruda
                    dto.setLat(lat);         // Calculado
                    dto.setLon(lon);         // Calculado

                    aeropuertos.put(codigoICAO, dto);
                } catch (NumberFormatException e) {
                    errores.add(new ErrorParseo(lineaNum, "Error en números: " + e.getMessage(), line));
                } catch (Exception e) {
                    errores.add(new ErrorParseo(lineaNum, "Error procesando: " + e.getMessage(), line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error en reader: " + e.getMessage(), e);
        }

        System.out.println("✅ Parseados " + aeropuertos.size() + " aeropuertos desde " + fuente + ". Errores: " + errores.size());
        if (!errores.isEmpty()) {
            System.err.println("Errores de parseo: " + errores);
        }
        return aeropuertos;
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    private static String safeLine(String line) {
        if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) return null;
        return line.trim();
    }

    private static int parseInt(String str, int lineaNum) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Línea " + lineaNum + ": '" + str + "' no es un entero válido");
        }
    }

    private static int parseTimeToMinutes(String timeStr, int lineaNum) {
        try {
            LocalTime time = LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return time.getHour() * 60 + time.getMinute();
        } catch (Exception e) {
            throw new IllegalArgumentException("Línea " + lineaNum + ": Hora inválida '" + timeStr + "' (formato HH:mm)");
        }
    }

    private static boolean isValidIata(String iata) {
        return iata != null && iata.matches("[A-Z]{3,4}");
    }

    // Método genérico para obtener errores (si quieres retornarlos junto con DTOs)
    public static Map<String, Object> getParseResultWithErrors(List<?> dtos, List<ErrorParseo> errores) {
        Map<String, Object> result = new HashMap<>();
        result.put("datos", dtos);
        result.put("errores", errores.stream().map(ErrorParseo::getMensaje).collect(Collectors.toList()));
        return result;
    }
}
