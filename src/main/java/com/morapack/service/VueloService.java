package com.morapack.service;


import com.morapack.dto.RespuestaDTO;
import com.morapack.dto.T06VueloProgramadoDto;
import com.morapack.dto.VueloInputDto;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T06VueloProgramado;
import com.morapack.model.T11Avion;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.T06VueloProgramadoRepository;
import com.morapack.repository.T11AvionRepository;
import com.morapack.util.UtilArchivos;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VueloService {

    @Autowired
    private T01AeropuertoRepository aeropuertoRepository;
    @Autowired
    private T06VueloProgramadoRepository vueloProgramadoRepository;
    @Autowired
    private T11AvionRepository avionRepository;

    private static final String MATRICULA_GENERICA = "MAT-GEN";

    private static final LocalDateTime FECHA_BASE = LocalDateTime.of(2025, 1, 1, 0, 0); // Fecha de referencia fija


    // ========================================================================
    // LÓGICA CENTRAL DE PROCESAMIENTO
    // ========================================================================

    @Transactional
    public RespuestaDTO cargarVueloFormulario(@Valid VueloInputDto inputDto, BindingResult bindingResult) {
        // 0. Validación de formato @Pattern del DTO
        if (bindingResult.hasErrors()) {
            // ... manejo de errores ...
            String errores = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return new RespuestaDTO("error", "Validación fallida: " + errores, null);
        }

        try {
            // Conversión de horas para el DTO
            inputDto.setHSalidaMinutos(UtilArchivos.parseTimeToMinutes(inputDto.getHoraSalidaStr()));
            inputDto.setHLlegadaMinutos(UtilArchivos.parseTimeToMinutes(inputDto.getHoraLlegadaStr()));

            return procesarVuelo(inputDto);

        } catch (IllegalArgumentException e) {
            return new RespuestaDTO("error", "Error de parseo de hora: " + e.getMessage(), null);
        } catch (Exception e) {
            return new RespuestaDTO("error", "Error interno al cargar vuelo: " + e.getMessage(), null);
        }
    }

    @Transactional
    public RespuestaDTO cargarVuelosArchivo(MultipartFile archivo) {
        try {
            // Asumo que UtilArchivos.cargarVuelos maneja correctamente el parseo de "0300" a Integer 300.
            List<VueloInputDto> inputDtos = UtilArchivos.cargarVuelos(archivo);
            List<String> errores = new ArrayList<>();

            // Esta lista solo la usaremos para contar
            int totalGuardados = 0;

            for (VueloInputDto inputDto : inputDtos) {

                try {
                    // El método procesarVuelo ahora maneja unicidad, crea el Instant y devuelve el DTO de SALIDA.
                    RespuestaDTO resultado = procesarVuelo(inputDto);

                    if ("error".equals(resultado.getStatus())) {
                        // Estado "error" si falló la búsqueda de aeropuerto o la validación.
                        errores.add(String.format("%s-%s: %s", inputDto.getIcaoOrigen(), inputDto.getIcaoDestino(), resultado.getMensaje()));
                    } else if ("warning".equals(resultado.getStatus())) {
                        // Estado "warning" si se detectó duplicado (unicidad).
                        errores.add(String.format("%s-%s: ADVERTENCIA - %s", inputDto.getIcaoOrigen(), inputDto.getIcaoDestino(), resultado.getMensaje()));
                    } else if ("success".equals(resultado.getStatus())) {
                        totalGuardados++;
                    }
                } catch (Exception e) {
                    errores.add(String.format("%s-%s: Error interno al procesar - %s", inputDto.getIcaoOrigen(), inputDto.getIcaoDestino(), e.getMessage()));
                }
            }

            Map<String, Object> data = Map.of(
                    "totalProcesados", inputDtos.size(),
                    "totalGuardados", totalGuardados,
                    "errores", errores
            );

            return new RespuestaDTO("success", String.format("Carga masiva completada. %d guardados, %d errores/advertencias.", totalGuardados, errores.size()), data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error procesando archivo de vuelos: " + e.getMessage(), null);
        }
    }

    public RespuestaDTO obtenerTodosVuelos() {
        try {
            // 1. Obtener todas las entidades
            // NOTA: findAll() hace un JOIN implícito para obtener los ICAO, lo cual es eficiente.
            List<T06VueloProgramado> vuelos = vueloProgramadoRepository.findAll();

            // 2. Mapear la lista de entidades a la lista de DTOs de salida
            List<T06VueloProgramadoDto> dtos = vuelos.stream()
                    .map(this::vueloToSalidaDto)
                    .toList();

            // 3. Respuesta
            Map<String, Object> data = Map.of(
                    "vuelos", dtos,
                    "total", dtos.size()
            );

            return new RespuestaDTO("success",
                    String.format("Se encontraron %d vuelos programados.", dtos.size()),
                    data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error al obtener la lista de vuelos: " + e.getMessage(), null);
        }
    }

    public RespuestaDTO obtenerVuelosPorAeropuerto(String icaoCodigo) {
        try {
            // 1. Buscar la entidad Aeropuerto por ICAO
            Optional<T01Aeropuerto> aeropuertoOpt = aeropuertoRepository.findByT01Codigoicao(icaoCodigo);

            if (aeropuertoOpt.isEmpty()) {
                return new RespuestaDTO("error", "Aeropuerto ICAO no encontrado: " + icaoCodigo, null);
            }

            T01Aeropuerto aeropuerto = aeropuertoOpt.get();

            // 2. Buscar vuelos por origen O destino
            List<T06VueloProgramado> vuelos = vueloProgramadoRepository
                    .buscarPorOrigenODestino(aeropuerto);

            // 3. Mapear la lista de entidades a la lista de DTOs de salida
            List<T06VueloProgramadoDto> dtos = vuelos.stream()
                    .map(this::vueloToSalidaDto)
                    .toList();

            // 4. Respuesta
            Map<String, Object> data = Map.of(
                    "vuelos", dtos,
                    "total", dtos.size()
            );

            return new RespuestaDTO("success",
                    String.format("Se encontraron %d vuelos programados para el aeropuerto %s.", dtos.size(), icaoCodigo),
                    data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error al obtener vuelos por aeropuerto: " + e.getMessage(), null);
        }
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    private RespuestaDTO procesarVuelo(VueloInputDto inputDto) {

        // 1. Buscar Aeropuertos (Origen y Destino)
        Optional<T01Aeropuerto> origenOpt = aeropuertoRepository.findByT01Codigoicao(inputDto.getIcaoOrigen());
        Optional<T01Aeropuerto> destinoOpt = aeropuertoRepository.findByT01Codigoicao(inputDto.getIcaoDestino());

        if (origenOpt.isEmpty() || destinoOpt.isEmpty()) {
            return new RespuestaDTO("error", "Aeropuerto ICAO no encontrado: " +
                    (origenOpt.isEmpty() ? inputDto.getIcaoOrigen() : inputDto.getIcaoDestino()), null);
        }

        T01Aeropuerto origen = origenOpt.get();
        T01Aeropuerto destino = destinoOpt.get();
        T11Avion avion = crearOObtenerAvionGenerico(inputDto.getCapacidad());

        // ********* INICIO DE VALIDACIÓN DE UNICIDAD *********

        List<T06VueloProgramado> vuelosExistentes = vueloProgramadoRepository
                .findByT01IdaeropuertoorigenAndT01IdaeropuertodestinoAndT06Capacidadtotal(
                        origen, destino, inputDto.getCapacidad());

        // Usaremos los minutos de entrada para verificar la duplicidad
        Integer inputSalidaMinutos = inputDto.getHSalidaMinutos();
        Integer inputLlegadaMinutos = inputDto.getHLlegadaMinutos();

        for (T06VueloProgramado vuelo : vuelosExistentes) {
            // La lógica de extracción de minutos de la DB es CORRECTA si la hora se guardó en UTC.

            Instant dbSalidaInstant = vuelo.getT06Fechasalida();
            Instant dbLlegadaInstant = vuelo.getT06Fechallegada();

            // Convertir Instant de DB a minutos del día (ASUMIENDO QUE FUE GUARDADO EN UTC)
            int dbSalidaMinutos = LocalDateTime.ofInstant(dbSalidaInstant, ZoneOffset.UTC).toLocalTime().toSecondOfDay() / 60;
            int dbLlegadaMinutos = LocalDateTime.ofInstant(dbLlegadaInstant, ZoneOffset.UTC).toLocalTime().toSecondOfDay() / 60;

            // Comparamos los minutos de salida y llegada.
            if (dbSalidaMinutos == inputSalidaMinutos && dbLlegadaMinutos == inputLlegadaMinutos) {
                return new RespuestaDTO("warning",
                        String.format("Vuelo duplicado encontrado: %s a %s, %s-%s, Capacidad: %d.",
                                inputDto.getIcaoOrigen(), inputDto.getIcaoDestino(),
                                inputDto.getHoraSalidaStr(), inputDto.getHoraLlegadaStr(),
                                inputDto.getCapacidad()),
                        null);
            }
        }

        // ********* FIN DE VALIDACIÓN DE UNICIDAD *********

        Instant horaSalida = crearInstantSimple(inputDto.getHoraSalidaStr());
        Instant horaLlegada = crearInstantSimple(inputDto.getHoraLlegadaStr());

        // 3. Crear el Vuelo Programado.
        T06VueloProgramado vuelo = new T06VueloProgramado();
        vuelo.setT01Idaeropuertoorigen(origen);
        vuelo.setT01Idaeropuertodestino(destino);
        vuelo.setT11Idavion(avion);
        vuelo.setT06Fechasalida(horaSalida); // Hora simple aplicada a la fecha base (en UTC)
        vuelo.setT06Fechallegada(horaLlegada); // Hora simple aplicada a la fecha base (en UTC)

        // 4. Setear campos de capacidad
        vuelo.setT06Capacidadtotal(inputDto.getCapacidad());

        T06VueloProgramado vueloGuardado = vueloProgramadoRepository.save(vuelo);

        // 6. Respuesta - ¡Devuelve el DTO de SALIDA!
        T06VueloProgramadoDto outputDto = vueloToSalidaDto(vueloGuardado);

        return new RespuestaDTO("success", "Vuelo programado guardado exitosamente.", outputDto);
    }

    private Instant crearInstantSimple(String horaStr) {
        // 1. Parsear la hora local (03:34)
        LocalTime horaLocal = LocalTime.parse(horaStr, DateTimeFormatter.ofPattern("HH:mm"));

        // 2. Combinar con la fecha base
        LocalDateTime dateTimeLocal = FECHA_BASE.withHour(horaLocal.getHour())
                .withMinute(horaLocal.getMinute())
                .withSecond(0)
                .withNano(0);

        // 3. Convertir LocalDateTime a Instant usando el offset UTC fijo (ignorando la zona real).
        return dateTimeLocal.toInstant(ZoneOffset.UTC);
    }

    private T06VueloProgramadoDto vueloToSalidaDto(T06VueloProgramado entidad) {

        // El constructor de @Value requiere que los 9 campos se pasen en el orden de declaración
        return new T06VueloProgramadoDto(
                entidad.getId(),
                entidad.getT06Fechasalida(),
                entidad.getT06Fechallegada(),
                entidad.getT06Capacidadtotal(),
                entidad.getT06Ocupacionactual(),
                entidad.getT06Estado(),
                entidad.getT06Estadocapacidad(),

                // Campos ICAO extraídos de la relación (2 campos)
                entidad.getT01Idaeropuertoorigen().getT01Codigoicao(),
                entidad.getT01Idaeropuertodestino().getT01Codigoicao()
        );
    }

    private T11Avion crearOObtenerAvionGenerico(Integer capacidad) {
        String matricula = MATRICULA_GENERICA + "-" + capacidad;

        // Buscar por matrícula genérica
        Optional<T11Avion> avionOpt = avionRepository.findByT11Matricula(matricula);

        if (avionOpt.isPresent()) {
            return avionOpt.get();
        }

        // Crear nuevo avión genérico
        T11Avion nuevoAvion = new T11Avion();
        nuevoAvion.setT11Matricula(matricula);
        nuevoAvion.setT11Capacidadmax(capacidad);

        // Model y Operador son null o String vacío, según la definición de la entidad.
        nuevoAvion.setT11Modelo(null);
        nuevoAvion.setT11Operador(null);
        // T11Activo tiene un @ColumnDefault 'DISPONIBLE'

        return avionRepository.save(nuevoAvion);
    }
}
