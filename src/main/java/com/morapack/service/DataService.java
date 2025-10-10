package com.morapack.service;

import com.morapack.dto.AeropuertoDTO;
import com.morapack.dto.RespuestaDTO;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T08Ciudad;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.T08CiudadRepository; // Importar el repositorio de Ciudad
import com.morapack.util.UtilArchivos;
import jakarta.persistence.EntityNotFoundException; // Usar una excepción más específica
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para carga y gestión de aeropuertos (T01Aeropuerto).
 * Maneja formulario (DTO único), archivo (múltiples), y consultas.
 */
@Service
public class DataService {

    // --- CORRECCIÓN: Inyección por constructor (práctica recomendada) ---
    private final T01AeropuertoRepository aeropuertoRepository;
    private final T08CiudadRepository ciudadRepository;

    public DataService(T01AeropuertoRepository aeropuertoRepository, T08CiudadRepository ciudadRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.ciudadRepository = ciudadRepository;
    }

    /**
     * Carga un aeropuerto desde un formulario.
     */
    @Transactional
    public RespuestaDTO cargarAeropuertoFormulario(@Valid AeropuertoDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errores = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return new RespuestaDTO("error", "Validación fallida: " + errores, null);
        }

        try {
            // Unificar lógica de parseo y validación de coordenadas
            parseAndValidateCoordinates(dto);

            // Verificar duplicado por ICAO
            if (aeropuertoRepository.existsByT01Codigoicao(dto.getCodigoICAO())) {
                return new RespuestaDTO("error", "Aeropuerto con ICAO '" + dto.getCodigoICAO() + "' ya existe.", null);
            }

            // Convertir DTO a entidad (ahora maneja la búsqueda de Ciudad)
            T01Aeropuerto entidad = dtoToAeropuerto(dto);
            T01Aeropuerto guardado = aeropuertoRepository.save(entidad);

            AeropuertoDTO dtoGuardado = aeropuertoToDto(guardado);
            Map<String, Object> data = new HashMap<>();
            data.put("aeropuerto", dtoGuardado);

            return new RespuestaDTO("success", "Aeropuerto cargado exitosamente.", data);

        } catch (EntityNotFoundException e) {
            return new RespuestaDTO("error", e.getMessage(), null); // Error si la ciudad no existe
        } catch (IllegalArgumentException e) {
            return new RespuestaDTO("error", e.getMessage(), null); // Error en coordenadas
        }
    }


    /**
     * Carga múltiples aeropuertos desde un archivo.
     */
    @Transactional
    public RespuestaDTO cargarAeropuertosArchivo(MultipartFile archivo) {
        try {
            Map<String, AeropuertoDTO> dtosMap = UtilArchivos.cargarAeropuertos(archivo);
            List<T01Aeropuerto> entidadesParaGuardar = new ArrayList<>();
            List<String> errores = new ArrayList<>();
            int duplicados = 0;

            for (AeropuertoDTO dto : dtosMap.values()) {
                if (!validarDtoAeropuerto(dto, errores)) continue;

                if (aeropuertoRepository.existsByT01Codigoicao(dto.getCodigoICAO())) {
                    errores.add("Duplicado ICAO: " + dto.getCodigoICAO());
                    duplicados++;
                    continue;
                }

                try {
                    // La conversión ahora puede lanzar una excepción si la ciudad no se encuentra
                    entidadesParaGuardar.add(dtoToAeropuerto(dto));
                } catch (EntityNotFoundException e) {
                    errores.add("Error para ICAO " + dto.getCodigoICAO() + ": " + e.getMessage());
                }
            }

            List<T01Aeropuerto> guardados = aeropuertoRepository.saveAll(entidadesParaGuardar);

            Map<String, Object> data = new HashMap<>();
            data.put("totalCargados", guardados.size());
            data.put("totalProcesados", dtosMap.size());
            data.put("duplicadosIgnorados", duplicados);
            data.put("errores", errores);

            String mensaje = String.format("Archivo procesado: %d cargados, %d duplicados, %d errores.",
                    guardados.size(), duplicados, errores.size());

            return new RespuestaDTO("success", mensaje, data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error fatal procesando archivo: " + e.getMessage(), null);
        }
    }


    /**
     * Obtiene todos los aeropuertos.
     */
    public RespuestaDTO obtenerTodosAeropuertos() {
        try {
            List<T01Aeropuerto> entidades = aeropuertoRepository.findAll();
            if (entidades.isEmpty()) {
                return new RespuestaDTO("warning", "No se encontraron aeropuertos registrados.",
                        Map.of("total", 0, "aeropuertos", List.of()));
            }

            List<AeropuertoDTO> dtos = entidades.stream()
                    .map(this::aeropuertoToDto)
                    .toList();

            Map<String, Object> data = Map.of("total", dtos.size(), "aeropuertos", dtos);
            return new RespuestaDTO("success", "Aeropuertos obtenidos exitosamente.", data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error obteniendo aeropuertos: " + e.getMessage(), null);
        }
    }


    // ========================================================================
    // MÉTODOS PRIVADOS DE CONVERSIÓN Y VALIDACIÓN
    // ========================================================================

    /**
     * Convierte AeropuertoDTO a la entidad T01Aeropuerto.
     * Busca y asigna la entidad T08Ciudad relacionada.
     */
    private T01Aeropuerto dtoToAeropuerto(AeropuertoDTO dto) {
        // --- CORRECCIÓN: Buscar la entidad Ciudad usando su ID ---
        T08Ciudad ciudad = ciudadRepository.findById(dto.getIdCiudad())
                .orElseThrow(() -> new EntityNotFoundException("Ciudad con ID '" + dto.getIdCiudad() + "' no encontrada."));

        T01Aeropuerto entidad = new T01Aeropuerto();
        // El ID del aeropuerto no se asigna, la DB lo genera (AUTO_INCREMENT)
        entidad.setT01Codigoicao(dto.getCodigoICAO());
        entidad.setT01Pais(dto.getPais());
        entidad.setT01Alias(dto.getAlias());
        entidad.setT01Continente(dto.getContinente());
        entidad.setT01GmtOffset(dto.getGmtOffset());
        entidad.setT01Capacidad(dto.getCapacidad());
        entidad.setT01LatDms(dto.getLatDms());
        entidad.setT01LonDms(dto.getLonDms());
        entidad.setT01Lat(dto.getLat() != null ? dto.getLat() : BigDecimal.ZERO);
        entidad.setT01Lon(dto.getLon() != null ? dto.getLon() : BigDecimal.ZERO);

        // --- CORRECCIÓN: Asignar el objeto Ciudad completo a la relación ---
        entidad.setT01Idciudad(ciudad);

        return entidad;
    }

    /**
     * Convierte la entidad T01Aeropuerto a AeropuertoDTO.
     */
    private AeropuertoDTO aeropuertoToDto(T01Aeropuerto entidad) {
        AeropuertoDTO dto = new AeropuertoDTO();
        dto.setIdAeropuerto(entidad.getT0Idaeropuerto());
        dto.setCodigoICAO(entidad.getT01Codigoicao());
        dto.setPais(entidad.getT01Pais());
        dto.setAlias(entidad.getT01Alias());
        dto.setContinente(entidad.getT01Continente());
        dto.setGmtOffset(entidad.getT01GmtOffset());
        dto.setCapacidad(entidad.getT01Capacidad());
        dto.setLatDms(entidad.getT01LatDms());
        dto.setLonDms(entidad.getT01LonDms());
        dto.setLat(entidad.getT01Lat());
        dto.setLon(entidad.getT01Lon());

        // --- CORRECCIÓN: Obtener datos de la entidad Ciudad relacionada ---
        if (entidad.getT01Idciudad() != null) {
            dto.setIdCiudad(entidad.getT01Idciudad().getT08Idciudad());
            // Asumiendo que T01Aeropuerto NO tiene ciudadNombre y se obtiene de la relación
            dto.setCiudadNombre(entidad.getT01Idciudad().getT08Nombre());
        }

        return dto;
    }

    /**
     * Validación manual de campos críticos del DTO.
     */
    private boolean validarDtoAeropuerto(AeropuertoDTO dto, List<String> errores) {
        boolean esValido = true;
        if (dto.getCodigoICAO() == null || !dto.getCodigoICAO().matches("[A-Z]{4}")) {
            errores.add("ICAO inválido: " + dto.getCodigoICAO());
            esValido = false;
        }
        if (dto.getIdCiudad() == null || dto.getIdCiudad().isBlank()) {
            errores.add("ID de Ciudad es requerido para ICAO: " + dto.getCodigoICAO());
            esValido = false;
        }
        // ... (otras validaciones que necesites)
        return esValido;
    }

    /**
     * Parsea coordenadas DMS a decimal si es necesario y valida el resultado.
     */
    private void parseAndValidateCoordinates(AeropuertoDTO dto) throws IllegalArgumentException {
        if (dto.getLatDms() != null && !dto.getLatDms().isBlank() && (dto.getLat() == null || dto.getLat().compareTo(BigDecimal.ZERO) == 0)) {
            dto.setLat(UtilArchivos.parseDmsToDecimal(dto.getLatDms(), true));
            if (dto.getLat().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Error al parsear latitud DMS: " + dto.getLatDms());
            }
        }
        if (dto.getLonDms() != null && !dto.getLonDms().isBlank() && (dto.getLon() == null || dto.getLon().compareTo(BigDecimal.ZERO) == 0)) {
            dto.setLon(UtilArchivos.parseDmsToDecimal(dto.getLonDms(), false));
            if (dto.getLon().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Error al parsear longitud DMS: " + dto.getLonDms());
            }
        }
    }
}