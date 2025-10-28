package com.morapack.service;


import com.morapack.dto.Flight_instances_DTO;
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

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    public RespuestaDTO cargarVuelosArchivo(MultipartFile archivo) {
        try {
            // Asumo que UtilArchivos.cargarVuelos maneja correctamente el parseo de "0300" a Integer 300.
            List<VueloInputDto> inputDtos = UtilArchivos.cargarVuelos(archivo);
            List<String> errores = new ArrayList<>();

            // Esta lista solo la usaremos para contar
            int totalGuardados = 0;
            System.out.println("=== DEPURACIÓN DE ARCHIVO DE VUELOS ===");
            System.out.println("Líneas leídas del archivo: " + inputDtos.size());
            inputDtos.forEach(v -> System.out.println(v.getIcaoOrigen() + " → " + v.getIcaoDestino()));

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

    private Flight_instances_DTO flight_instances_DTO(T06VueloProgramado vuelo) {
        // 1. Preparar formatos y datos base
        // Usamos el formatter ISO 8601 (el formato que usa la Z)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // Asumiendo que T06VueloProgramado tiene las relaciones a las entidades Aeropuerto
        // y que estas entidades tienen el campo T01_codigoIcao.
        String codigoIcaoOrigen = vuelo.getT01Idaeropuertoorigen().getT01Codigoicao();
        String codigoIcaoDestino = vuelo.getT01Idaeropuertodestino().getT01Codigoicao();

        // 2. Generar campos de Flight
        // MP-XXX (ej. MP-001)
        String flightId = "MP-" + String.format("%03d", vuelo.getId());

        // Formatear fechas a UTC (Z)
        String depUtc = vuelo.getT06Fechasalida().atZone(ZoneOffset.UTC).format(formatter);
        String arrUtc = vuelo.getT06Fechallegada().atZone(ZoneOffset.UTC).format(formatter);

        // 3. Construir Instance ID
        // MP-"IDDEVUELO"#"AÑO"-"MES"-"DIA"T"HORA",00Z (El formato de hora debe ser 24h)
        // Usamos la fecha de salida (depUtc) para el Instance ID, simplificando la cadena a la hora
        // exacta que necesitamos antes de la Z.
        String depTimeForInstanceId = vuelo.getT06Fechasalida().atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); // Sin segundos ni Z

        // El formato solicitado tiene una coma y dos ceros (",00Z"), que parece un error tipográfico.
        // Lo más cercano al estándar ISO 8601 es terminar en .000Z
        String instanceId = String.format("MP-%03d#%s:00.000Z",
                vuelo.getId(),
                depTimeForInstanceId);

        // 4. Crear y devolver el DTO
        return new Flight_instances_DTO(
                instanceId,
                flightId,
                codigoIcaoOrigen,
                codigoIcaoDestino,
                depUtc,
                arrUtc,
                vuelo.getT06Capacidadtotal()
        );
    }

    public RespuestaDTO obtenerFlightsDTO() {
        try {
            // 1. Obtener todas las entidades
            List<T06VueloProgramado> vuelos = vueloProgramadoRepository.findAll();

            // 2. Mapear la lista de entidades al DTO de salida (VueloSalidaDto)
            List<Flight_instances_DTO> dtos = vuelos.stream()
                    .map(this::flight_instances_DTO) // Usa el nuevo método de mapeo
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
            // Manejo de errores, incluyendo posibles NullPointer si las relaciones de Aeropuerto son nulas
            return new RespuestaDTO("error", "Error al obtener la lista de vuelos: " + e.getMessage(), null);
        }
    }

    public RespuestaDTO obtenerFlightsDTO2(int page, int size) {
        try {
            // 1. Obtener la data base (2866 vuelos) UNA SOLA VEZ.
            List<T06VueloProgramado> vuelosBase = vueloProgramadoRepository.findAll();

            // Lista para almacenar TODOS los 20,000+ vuelos generados
            List<Flight_instances_DTO> todosLosVuelos = new ArrayList<>();
            long flightIdCounter = 1; // Contador global para el nuevo Flight ID

            // La fecha de inicio es HOY (usando la zona UTC para que sea consistente)
            LocalDate fechaInicio = LocalDate.now(ZoneOffset.UTC);

            // Definir formatos
            // Formato de salida requerido para depUtc y arrUtc (ISO 8601 con Z)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            // Formato para el Instance ID (sin segundos ni milisegundos)
            DateTimeFormatter instanceIdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

            // 2. Iterar 7 veces (para los 7 días de la semana)
            for (int i = 0; i < 7; i++) {
                LocalDate fechaActual = fechaInicio.plusDays(i);

                // 3. Iterar sobre la lista base de vuelos para cada día
                for (T06VueloProgramado vueloBase : vuelosBase) {

                    // --- 3.a) Obtener datos y Offsets ---
                    T01Aeropuerto aeropuertoOrigen = vueloBase.getT01Idaeropuertoorigen();
                    T01Aeropuerto aeropuertoDestino = vueloBase.getT01Idaeropuertodestino();

                    if (aeropuertoOrigen == null || aeropuertoDestino == null) {
                        throw new IllegalStateException("Aeropuerto no cargado para el vuelo ID: " + vueloBase.getId());
                    }

                    // Se asume que getT01GmtOffset() devuelve un Integer o Short (ej. -5, 3)
                    ZoneOffset offsetOrigen = ZoneOffset.ofHours(aeropuertoOrigen.getT01GmtOffset());
                    ZoneOffset offsetDestino = ZoneOffset.ofHours(aeropuertoDestino.getT01GmtOffset());

                    // --- 3.b) Conversión y Cálculo de Fechas (usando ZonedDateTime) ---

                    // 1. Hora de Salida Base (Asumiendo que T06Fechasalida es Instant en la entidad)
                    ZonedDateTime zdtSalidaBase = vueloBase.getT06Fechasalida().atZone(offsetOrigen);

                    // 2. Hora de Llegada Base
                    ZonedDateTime zdtLlegadaBase = vueloBase.getT06Fechallegada().atZone(offsetDestino);

                    // 3. Reemplazar la fecha de la hora base con la fecha de la iteración,
                    //    manteniendo la HORA y el OFFSET (ZDT en hora local)
                    ZonedDateTime zdtSalidaDiaActual = zdtSalidaBase
                            .withDayOfMonth(fechaActual.getDayOfMonth())
                            .withMonth(fechaActual.getMonthValue())
                            .withYear(fechaActual.getYear());

                    // 4. Calcular la duración del vuelo base y aplicarla a la nueva hora de salida
                    long durationSeconds = ChronoUnit.SECONDS.between(zdtSalidaBase, zdtLlegadaBase);
                    ZonedDateTime zdtLlegadaDiaActual = zdtSalidaDiaActual.plusSeconds(durationSeconds);


                    // --- 3.c) Generación de Strings y IDs ---

                    // Formato UTC final para el DTO (ej. "2025-10-20T06:58:00.000Z")
                    String depUtc = zdtSalidaDiaActual.withZoneSameInstant(ZoneOffset.UTC).format(formatter);
                    String arrUtc = zdtLlegadaDiaActual.withZoneSameInstant(ZoneOffset.UTC).format(formatter);

                    // IDs
                    String flightIdBase = String.format("MP-%03d", vueloBase.getId());
                    String flightIdNuevo = "MP-" + String.format("%04d", flightIdCounter);

                    // Instance ID (ej. "MP-001#2025-10-20T01:58:00.000Z")
                    String depTimeForInstanceId = zdtSalidaDiaActual.format(instanceIdFormatter);

                    String instanceId = String.format("%s#%s:00.000Z",
                            flightIdBase,
                            depTimeForInstanceId);

                    // 3.d) Construir y agregar DTO a la lista maestra
                    Flight_instances_DTO dto = new Flight_instances_DTO(
                            instanceId,
                            flightIdNuevo,
                            aeropuertoOrigen.getT01Codigoicao(),
                            aeropuertoDestino.getT01Codigoicao(),
                            depUtc,
                            arrUtc,
                            vueloBase.getT06Capacidadtotal()
                    );

                    todosLosVuelos.add(dto);
                    flightIdCounter++;
                }
            }

            // --- 4. LÓGICA DE PAGINACIÓN ---

            int totalItems = todosLosVuelos.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalItems);

            // Validar límites de página
            if (startIndex >= totalItems || startIndex < 0 || size <= 0) {
                // Devolver la estructura esperada con una lista vacía
                return new RespuestaDTO("success",
                        "No hay más vuelos o parámetros inválidos.",
                        Map.of("vuelos", Collections.emptyList(),
                                "totalItems", totalItems,
                                "paginaActual", page));
            }

            // Obtener el subconjunto (página) de vuelos
            List<Flight_instances_DTO> dtosPagina = todosLosVuelos.subList(startIndex, endIndex);

            // 5. Respuesta de la página actual
            Map<String, Object> data = Map.of(
                    "vuelos", dtosPagina,
                    "totalItems", totalItems,
                    "itemsEnPagina", dtosPagina.size(),
                    "paginaActual", page
            );

            return new RespuestaDTO("success",
                    String.format("Página %d: Mostrando %d vuelos de %d totales.", page, dtosPagina.size(), totalItems),
                    data);

        } catch (Exception e) {
            e.printStackTrace();
            return new RespuestaDTO("error", "Error al obtener la lista de vuelos: " + e.getMessage(), null);
        }
    }


    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    @Transactional
    RespuestaDTO procesarVuelo(VueloInputDto inputDto) {

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

        // 4. Setear campos de capacidad y estados
        vuelo.setT06Capacidadtotal(inputDto.getCapacidad());
        vuelo.setT06Ocupacionactual(0); // Inicialmente sin ocupación
        vuelo.setT06Estado("PROGRAMADO"); // Estado inicial del vuelo
        vuelo.setT06Estadocapacidad("NORMAL"); // Estado de capacidad inicial

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
        // T11Activo debe ser seteado explícitamente (el @ColumnDefault no funciona en JPA para inserts)
        nuevoAvion.setT11Activo("DISPONIBLE");

        return avionRepository.save(nuevoAvion);
    }
}
