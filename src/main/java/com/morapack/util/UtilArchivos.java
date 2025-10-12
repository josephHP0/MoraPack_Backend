package com.morapack.util;

import com.morapack.dto.AeropuertoInputDto;
import com.morapack.dto.PedidoInputDto;
import com.morapack.dto.VueloInputDto;

import lombok.Data;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.LocalTime;


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

    public static BigDecimal parseDmsToDecimal(String dmsStr, boolean isLat) {
        if (dmsStr == null || dmsStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            String cleanDms = dmsStr.replaceAll("\\s+", "");

            // Pattern: Captura \d+°\d+'\d+"[NSEW]. Se usa \\" para escapar la comilla doble
            Pattern pattern = Pattern.compile("^(\\d+)°(\\d+)'(\\d+)\"(\\p{Lu})$");
            Matcher matcher = pattern.matcher(cleanDms);

            if (!matcher.matches()) {
                throw new IllegalArgumentException("Formato DMS inválido: " + dmsStr + " (esperado: dd°mm'ss\"[NSEW])");
            }

            BigDecimal grados = new BigDecimal(matcher.group(1));
            final int INTERMEDIATE_SCALE = 10;
            
            BigDecimal minutos = new BigDecimal(matcher.group(2))
                .divide(BigDecimal.valueOf(60), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
                
            BigDecimal segundos = new BigDecimal(matcher.group(3))
                .divide(BigDecimal.valueOf(3600), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
                
            String direccion = matcher.group(4).toUpperCase();

            BigDecimal decimal = grados.add(minutos).add(segundos);

            if ((isLat && "S".equals(direccion)) || (!isLat && "W".equals(direccion))) {
                decimal = decimal.negate();
            }

            return decimal.setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando DMS '" + dmsStr + "': " + e.getMessage(), e);
        }
    }

    public static List<VueloInputDto> cargarVuelos(MultipartFile archivo) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            return parsearVuelos(br, archivo.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivo de vuelos: " + e.getMessage(), e);
        }
    }

    private static List<VueloInputDto> parsearVuelos(BufferedReader br, String fuente) {
        List<VueloInputDto> dtos = new ArrayList<>();
        List<ErrorParseo> errores = new ArrayList<>(); // Si quieres manejar errores

        String line;
        int lineaNum = 0;

        try {
            while ((line = br.readLine()) != null) {
                lineaNum++;
                String lineaLimpia = line.trim();
                if (lineaLimpia.isEmpty()) continue;

                String[] parts = lineaLimpia.split(",", -1);
                if (parts.length != 5) {
                    // errores.add(new ErrorParseo(lineaNum, "Se esperan 5 campos", line));
                    continue;
                }

                try {
                    VueloInputDto dto = new VueloInputDto();
                    dto.setIcaoOrigen(parts[0].trim());
                    dto.setIcaoDestino(parts[1].trim());
                    dto.setHoraSalidaStr(parts[2].trim());
                    dto.setHoraLlegadaStr(parts[3].trim());
                    dto.setCapacidad(Integer.valueOf(parts[4].trim()));

                    // Conversión a minutos (para validación en servicio si es necesario)
                    dto.setHSalidaMinutos(parseTimeToMinutes(dto.getHoraSalidaStr()));
                    dto.setHLlegadaMinutos(parseTimeToMinutes(dto.getHoraLlegadaStr()));

                    dtos.add(dto);

                } catch (IllegalArgumentException e) {
                    errores.add(new ErrorParseo(lineaNum, "Error en datos: " + e.getMessage(), line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error en reader de vuelos: " + e.getMessage(), e);
        }
        return dtos;
    }

    public static List<AeropuertoInputDto> cargarAeropuertos(Path path) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            return parsearAeropuertos(br, path.toString());
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo: " + e.getMessage(), e);
        }
    }

    public static List<AeropuertoInputDto> cargarAeropuertos(MultipartFile archivo) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            return parsearAeropuertos(br, archivo.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivo: " + e.getMessage(), e);
        }
    }

    private static List<AeropuertoInputDto> parsearAeropuertos(BufferedReader br, String fuente) {
        List<AeropuertoInputDto> dtos = new ArrayList<>();
        List<ErrorParseo> errores = new ArrayList<>();
        String line;
        int lineaNum = 0;

        try {
            while ((line = br.readLine()) != null) {
                lineaNum++;
                String lineaLimpia = safeLine(line);
                if (lineaLimpia == null) continue;

                String[] parts = lineaLimpia.split(",", -1);
                if (parts.length != 9) {
                    errores.add(new ErrorParseo(lineaNum, "Se esperan 9 campos", line));
                    continue;
                }

                try {
                    String icao = parts[0].trim();
                    String nombreCiudad = parts[1].trim();
                    String nombrePais = parts[2].trim();
                    String alias = parts[3].trim();
                    Short gmtOffset = Short.valueOf(parts[4].trim());
                    int capacidad = Integer.parseInt(parts[5].trim());
                    String latDms = parts[6].trim();
                    String lonDms = parts[7].trim();
                    String continente = parts[8].trim().toUpperCase();

                    // Validaciones básicas (se mantienen)
                    if (!icao.matches("[A-Z]{4}")) {
                        errores.add(new ErrorParseo(lineaNum, "ICAO inválido: " + icao, line));
                        continue;
                    }
                    if (capacidad <= 0) {
                        errores.add(new ErrorParseo(lineaNum, "Capacidad debe ser >0", line));
                        continue;
                    }
                    
                    // Crear DTO con campos STRING y de valor
                    AeropuertoInputDto dto = new AeropuertoInputDto();
                    dto.setNombreCiudad(nombreCiudad);
                    dto.setContinenteCiudad(continente);
                    dto.setCodigoICAO(icao);
                    dto.setGmtOffset(gmtOffset);
                    dto.setCapacidad(capacidad);
                    dto.setAlias(alias.isEmpty() ? null : alias);
                    dto.setPaisCiudad(nombrePais);
                    
                    // Asignar DMS crudo (para validación de DTO en servicio)
                    dto.setLatDms(latDms);
                    dto.setLonDms(lonDms);
                    
                    // *** CORRECCIÓN: Parsear DMS a decimal y manejar excepción ***
                    // Esto SETEA los campos BigDecimal lat/lon
                    try {
                        dto.setLat(parseDmsToDecimal(latDms, true));
                    } catch (IllegalArgumentException e) {
                        errores.add(new ErrorParseo(lineaNum, "Error latDms: " + e.getMessage(), line));
                        continue;
                    }
                    
                    try {
                        dto.setLon(parseDmsToDecimal(lonDms, false));
                    } catch (IllegalArgumentException e) {
                        errores.add(new ErrorParseo(lineaNum, "Error lonDms: " + e.getMessage(), line));
                        continue;
                    }

                    try{
                        //Validar que no se repita el aeropuerto  en la base de datos
                        if(dtos.stream().anyMatch(d -> d.getCodigoICAO().equals(dto.getCodigoICAO()))){
                            errores.add(new ErrorParseo(lineaNum, "El aeropuerto con ICAO: " + dto.getCodigoICAO() + " ya fue ingresado anteriormente", line));
                            continue;
                        }                       
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    dtos.add(dto);
                } catch (NumberFormatException e) {
                    errores.add(new ErrorParseo(lineaNum, "Error en números: " + e.getMessage(), line));
                } catch (Exception e) {
                    errores.add(new ErrorParseo(lineaNum, "Error procesando: " + e.getMessage(), line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error en reader: " + e.getMessage(), e);
        }

        System.out.println("✅ Parseados " + dtos.size() + " aeropuertos desde " + fuente + ". Errores: " + errores.size());
        if (!errores.isEmpty()) {
            System.err.println("Errores: " + errores.stream().map(ErrorParseo::getMensaje).collect(Collectors.joining("; ")));
        }
        return dtos;
    }

    public static List<PedidoInputDto> cargarPedidos(MultipartFile archivo) throws IOException {
        List<PedidoInputDto> pedidos = new ArrayList<>();

        // Verifica que el archivo no esté vacío
        if (archivo.isEmpty()) {
            return pedidos;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            int contadorLinea = 0;

            while ((linea = br.readLine()) != null) {
                contadorLinea++;

                // Ignorar líneas vacías o de encabezado
                if (linea.trim().isEmpty() || contadorLinea == 1) {
                    continue;
                }

                String[] partes = linea.split("-");

                if (partes.length != 6) {
                    // Si el formato es incorrecto, lanzamos una excepción o registramos el error
                    // En carga masiva, es mejor registrar y continuar, pero para simplicidad, lanzaremos un error.
                    throw new IllegalArgumentException("Línea " + contadorLinea + ": Formato incorrecto. Se esperaban 6 campos, se encontraron " + partes.length + ".");
                }

                try {
                    PedidoInputDto dto = new PedidoInputDto();

                    dto.setDia(Integer.valueOf(partes[0].trim()));
                    dto.setHora(Integer.valueOf(partes[1].trim()));
                    dto.setMinuto(Integer.valueOf(partes[2].trim()));
                    dto.setIcaoDestino(partes[3].trim().toUpperCase());
                    dto.setTamanho(Integer.valueOf(partes[4].trim()));
                    dto.setNombreCliente(partes[5].trim()); // Usando el último dato como nombre

                    pedidos.add(dto);

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Línea " + contadorLinea + ": Error al parsear un campo numérico. Verifique día, hora, minuto o tamaño.");
                }
            }
        }

        return pedidos;
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

    public static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Hora vacía.");
        }
        try {
            LocalTime time = LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return time.getHour() * 60 + time.getMinute();
        } catch (Exception e) {
            throw new IllegalArgumentException("Hora inválida '" + timeStr + "' (formato HH:mm)");
        }
    }

    // Método genérico para obtener errores (si quieres retornarlos junto con DTOs)
    public static Map<String, Object> getParseResultWithErrors(List<?> dtos, List<ErrorParseo> errores) {
        Map<String, Object> result = new HashMap<>();
        result.put("datos", dtos);
        result.put("errores", errores.stream().map(ErrorParseo::getMensaje).collect(Collectors.toList()));
        return result;
    }
}
