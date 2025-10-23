package com.morapack.service;

import com.morapack.dto.AeropuertoInputDto;
import com.morapack.dto.RespuestaDTO;
import com.morapack.dto.T01AeropuertoDto;
import com.morapack.dto.T08CiudadDto;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T08Ciudad;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.T08CiudadRepository;
import com.morapack.util.UtilArchivos;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.morapack.dto.AeropuertoOutputDto;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;

@Service
public class AeropuertoService {

    @Autowired
    private T08CiudadRepository ciudadRepository;
    @Autowired
    private T01AeropuertoRepository aeropuertoRepository;

    private static final Set<String> CODIGOS_HUB = Set.of("SPIM", "UBBB", "EBCI");

    // ========================================================================
    // LÓGICA CENTRAL DE PROCESAMIENTO
    // ========================================================================

    @Transactional
    public RespuestaDTO cargarAeropuertoFormulario(AeropuertoInputDto inputDto, BindingResult bindingResult) {

        // 0. Validación de formato @Pattern del DTO
        if (bindingResult.hasErrors()) {
            String errores = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return new RespuestaDTO("error", "Validación fallida: " + errores, null);
        }

        List<String> erroresDms = new ArrayList<>();

        // Paso 1: Parsear DMS a lat/lon usando la utilidad
        if (inputDto.getLatDms() != null && !inputDto.getLatDms().isEmpty()) {
            try {
                // *** CORREGIDO: Llama a setLat(BigDecimal) y usa UtilArchivos ***
                inputDto.setLat(UtilArchivos.parseDmsToDecimal(inputDto.getLatDms(), true));
            } catch (IllegalArgumentException e) {
                erroresDms.add("Latitud DMS inválida: " + inputDto.getLatDms());
            }
        }

        if (inputDto.getLonDms() != null && !inputDto.getLonDms().isEmpty()) {
            try {
                // *** CORREGIDO: Llama a setLon(BigDecimal) y usa UtilArchivos ***
                inputDto.setLon(UtilArchivos.parseDmsToDecimal(inputDto.getLonDms(), false));
            } catch (IllegalArgumentException e) {
                erroresDms.add("Longitud DMS inválida: " + inputDto.getLonDms());
            }
        }

        // Validar y devolver errores de parseo
        if (!erroresDms.isEmpty()) {
            return new RespuestaDTO("error", "Errores en coordenadas DMS: " + String.join(", ", erroresDms), null);
        }

        try {
            // Paso 2: Crear/buscar Ciudad
            T08Ciudad ciudad = crearOObtenerCiudad(inputDto);
            if (ciudad == null) {
                return new RespuestaDTO("error", "Error creando/buscando ciudad: " + inputDto.getNombreCiudad(), null);
            }

            // Paso 3: Verificar duplicado Aeropuerto por ICAO
            if (aeropuertoRepository.existsByT01Codigoicao(inputDto.getCodigoICAO())) {
                return new RespuestaDTO("error", "Aeropuerto con ICAO '" + inputDto.getCodigoICAO() + "' ya existe", null);
            }

            // Paso 4: Crear y guardar Aeropuerto
            T01Aeropuerto aeropuerto = inputDtoToAeropuerto(inputDto, ciudad);
            T01Aeropuerto guardado = aeropuertoRepository.save(aeropuerto);

            // Paso 5: Respuesta con DTOs de salida
            T01AeropuertoDto aeropuertoDto = aeropuertoToDto(guardado);
            T08CiudadDto ciudadDto = ciudadToDto(ciudad);

            Map<String, Object> data = new HashMap<>();
            data.put("aeropuerto", aeropuertoDto);
            data.put("ciudad", ciudadDto);

            return new RespuestaDTO("success", "Aeropuerto y ciudad cargados exitosamente", data);
        } catch (Exception e) {
            return new RespuestaDTO("error", "Error interno: " + e.getMessage(), null);
        }
    }

    @Transactional
    public RespuestaDTO cargarAeropuertosArchivo(MultipartFile archivo) {
        try {
            // UtilArchivos.cargarAeropuertos(archivo) ya realiza el parseo DMS y maneja errores de línea
            List<AeropuertoInputDto> inputDtos = UtilArchivos.cargarAeropuertos(archivo);

            List<T01Aeropuerto> aeropuertosGuardados = new ArrayList<>();
            List<T08Ciudad> ciudadesGuardadas = new ArrayList<>();
            List<String> errores = new ArrayList<>();
            int duplicados = 0;
            Set<String> nombresCiudadesUnicos = new HashSet<>();

            for (AeropuertoInputDto inputDto : inputDtos) {
                try {
                    // *** CORREGIDO: Se elimina el parseo DMS y la validación de ZERO, ya hechos en UtilArchivos ***

                    // Crear/buscar Ciudad
                    if (!nombresCiudadesUnicos.contains(inputDto.getNombreCiudad())) {
                        T08Ciudad ciudad = crearOObtenerCiudad(inputDto);
                        if (ciudad != null) {
                            ciudadesGuardadas.add(ciudad);
                            nombresCiudadesUnicos.add(inputDto.getNombreCiudad());
                        } else {
                            errores.add("Error ciudad: " + inputDto.getNombreCiudad());
                            continue;
                        }
                    }

                    // Obtener Ciudad (ya creada/buscada)
                    Optional<T08Ciudad> ciudadOpt = ciudadRepository.findByT08Nombre(inputDto.getNombreCiudad());
                    if (ciudadOpt.isEmpty()) {
                        errores.add("Ciudad no encontrada: " + inputDto.getNombreCiudad());
                        continue;
                    }
                    T08Ciudad ciudad = ciudadOpt.get();

                    // Verificar duplicado Aeropuerto
                    if (aeropuertoRepository.existsByT01Codigoicao(inputDto.getCodigoICAO())) {
                        duplicados++;
                        continue;
                    }

                    // Crear y guardar Aeropuerto
                    T01Aeropuerto aeropuerto = inputDtoToAeropuerto(inputDto, ciudad);
                    T01Aeropuerto guardado = aeropuertoRepository.save(aeropuerto);
                    aeropuertosGuardados.add(guardado);

                } catch (Exception e) {
                    errores.add("Error procesando línea: " + e.getMessage());
                }
            }

            // Respuesta
            Map<String, Object> data = new HashMap<>();
            data.put("totalAeropuertosCargados", aeropuertosGuardados.size());
            data.put("totalCiudadesCargadas", ciudadesGuardadas.size());
            data.put("duplicadosIgnorados", duplicados);
            data.put("errores", errores);

            return new RespuestaDTO("success",
                    String.format("Archivo procesado: %d aeropuertos, %d ciudades, %d duplicados, %d errores",
                            aeropuertosGuardados.size(), ciudadesGuardadas.size(), duplicados, errores.size()),
                    data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error procesando archivo: " + e.getMessage(), null);
        }
    }

    public RespuestaDTO obtenerTodosAeropuertos() {
        try {
            List<T01Aeropuerto> entidades = aeropuertoRepository.findAll();
            List<T01AeropuertoDto> dtos = entidades.stream()
                    .map(this::aeropuertoToDto)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("total", dtos.size());
            data.put("aeropuertos", dtos);

            return new RespuestaDTO("success", "Aeropuertos obtenidos exitosamente", data);
        } catch (Exception e) {
            return new RespuestaDTO("error", "Error obteniendo aeropuertos: " + e.getMessage(), null);
        }
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    private T08Ciudad crearOObtenerCiudad(AeropuertoInputDto inputDto) {
        Optional<T08Ciudad> ciudadOpt = ciudadRepository.findByT08Nombre(inputDto.getNombreCiudad());
        if (ciudadOpt.isPresent()) {
            T08Ciudad ciudad = ciudadOpt.get();
            // Verificar consistencia (opcional)
            if (!inputDto.getContinenteCiudad().equals(ciudad.getT08Continente())) {
                System.err.println("Advertencia: Continente diferente para ciudad existente " + inputDto.getNombreCiudad());
                // No actualiza; usa existente
            }
            return ciudad;
        } else {
            // Crear nueva
            T08Ciudad nuevaCiudad = getT08Ciudad(inputDto);
            return ciudadRepository.save(nuevaCiudad);
        }
    }

    private T01Aeropuerto inputDtoToAeropuerto(AeropuertoInputDto inputDto, T08Ciudad ciudad) {
        T01Aeropuerto entidad = new T01Aeropuerto();
        entidad.setT08Idciudad(ciudad);
        entidad.setT01Codigoicao(inputDto.getCodigoICAO());

        // *** CORREGIDO: Usa los campos decimales (lat/lon) ya parseados ***
        entidad.setT01Lat(inputDto.getLat() != null ? inputDto.getLat() : BigDecimal.ZERO);
        entidad.setT01Lon(inputDto.getLon() != null ? inputDto.getLon() : BigDecimal.ZERO);

        entidad.setT01GmtOffset(inputDto.getGmtOffset());
        entidad.setT01Capacidad(inputDto.getCapacidad());
        entidad.setT01Alias(inputDto.getAlias());
        return entidad;
    }

    private T01AeropuertoDto aeropuertoToDto(T01Aeropuerto entidad) {
        T01AeropuertoDto dto = new T01AeropuertoDto(
                entidad.getId(),
                entidad.getT01Codigoicao(),
                entidad.getT01Lat(),
                entidad.getT01Lon(),
                entidad.getT01GmtOffset(),
                entidad.getT01Capacidad(),
                entidad.getT01Alias()
        );
        // Para embebida, usa un wrapper o expande T01AeropuertoDto con CiudadDto
        // Por simplicidad, retorna solo aeropuerto; expande si necesitas
        return dto;
    }

    private T08CiudadDto ciudadToDto(T08Ciudad entidad) {
        return new T08CiudadDto(
                entidad.getId(),
                entidad.getT08Nombre(),
                entidad.getT08Continente(),
                entidad.getT08Zonahoraria(),
                entidad.getT08Eshub()
        );
    }

    private static T08Ciudad getT08Ciudad(AeropuertoInputDto inputDto) {
        T08Ciudad nuevaCiudad = new T08Ciudad();
        nuevaCiudad.setT08Nombre(inputDto.getNombreCiudad());
        nuevaCiudad.setT08Continente(inputDto.getContinenteCiudad() != null ? inputDto.getContinenteCiudad() : "AM");  // Default AM
        nuevaCiudad.setT08Zonahoraria(inputDto.getZonaHorariaCiudad() != null ? inputDto.getZonaHorariaCiudad() : "UTC");  // Default
        boolean esHub = CODIGOS_HUB.contains(inputDto.getCodigoICAO());
        nuevaCiudad.setT08Eshub(esHub);
        return nuevaCiudad;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<AeropuertoOutputDto> listarAeropuertosParaApi() {
        List<T01Aeropuerto> entidades = aeropuertoRepository.findAll();
        return entidades.stream()
                .map(this::toOutputDto)
                .toList();
    }

    // ========================================================================
    // MÉTODOS GET PARA API (FRONT)
    // ========================================================================
    @Transactional(Transactional.TxType.SUPPORTS)
    public AeropuertoOutputDto obtenerAeropuertoPorIcao(String icao) {
        T01Aeropuerto a = aeropuertoRepository.findByT01Codigoicao(icao)
                .orElseThrow(() -> new NoSuchElementException("No existe aeropuerto con ICAO " + icao));
        return toOutputDto(a);
    }

    private AeropuertoOutputDto toOutputDto(T01Aeropuerto a) {
        T08Ciudad c = a.getT08Idciudad();

        String icao = a.getT01Codigoicao();
        String iata = null; // Opcional: podrías tener este campo en la BD
        String name = (a.getT01Alias() != null && !a.getT01Alias().isBlank()) ? a.getT01Alias() : icao;
        String city = (c != null && c.getT08Nombre() != null) ? c.getT08Nombre() : "";
        String country = ""; // Podrías agregar un campo país en T08Ciudad

        double lat = a.getT01Lat() != null ? a.getT01Lat().doubleValue() : 0.0;
        double lon = a.getT01Lon() != null ? a.getT01Lon().doubleValue() : 0.0;

        Integer warehouseCapacity = a.getT01Capacidad();
        Boolean infiniteSource = (c != null && c.getT08Eshub() != null) ? c.getT08Eshub() : false;

        return new AeropuertoOutputDto(
                icao, iata, name, city, country, lat, lon, warehouseCapacity, infiniteSource
        );
    }









}
