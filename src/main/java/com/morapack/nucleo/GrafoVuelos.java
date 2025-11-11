package com.morapack.nucleo;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T06VueloProgramado;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestiona el grafo de vuelos y genera instancias concretas para una ventana temporal.
 *
 * Responsabilidades:
 * - Expandir plantillas de vuelos (T06VueloProgramado) en instancias diarias (FlightInstance)
 * - Crear índices eficientes por ID, por salidas/llegadas por aeropuerto
 * - Gestionar capacidades remanentes durante la simulación
 *
 * Basado en GrafoVuelos del proyecto de referencia, adaptado para trabajar con JPA entities.
 */
@Slf4j
public class GrafoVuelos {

    // ========== Datos de entrada ==========

    /** Vuelos programados de la BD */
    private final List<T06VueloProgramado> vuelosProgramados;

    /** Mapa de aeropuertos por ID */
    private final Map<Integer, T01Aeropuerto> aeropuertosPorId;

    /** Mapa de aeropuertos por código ICAO */
    private final Map<String, T01Aeropuerto> aeropuertosPorCodigo;

    // ========== Instancias expandidas ==========

    /** Todas las instancias de vuelo generadas */
    private List<FlightInstance> instancias;

    /** Índice por ID de instancia */
    private Map<String, FlightInstance> byId;

    /** Vuelos salientes por aeropuerto (ordenados por hora de salida) */
    private Map<String, List<FlightInstance>> outByAp;

    /** Vuelos entrantes por aeropuerto (ordenados por hora de llegada) */
    private Map<String, List<FlightInstance>> inByAp;

    // ========== Estado de capacidades ==========

    /** Capacidad remanente por vuelo (actualizada durante simulación) */
    private Map<String, Integer> capRemain;

    /**
     * Constructor.
     *
     * @param vuelosProgramados Lista de vuelos de la BD
     * @param aeropuertos Lista de aeropuertos
     */
    public GrafoVuelos(List<T06VueloProgramado> vuelosProgramados, List<T01Aeropuerto> aeropuertos) {
        this.vuelosProgramados = vuelosProgramados;

        // Crear mapas de aeropuertos
        this.aeropuertosPorId = new HashMap<>();
        this.aeropuertosPorCodigo = new HashMap<>();
        for (T01Aeropuerto ap : aeropuertos) {
            aeropuertosPorId.put(ap.getId(), ap);
            aeropuertosPorCodigo.put(ap.getT01Codigoicao(), ap);
        }

        this.instancias = new ArrayList<>();
        this.byId = new HashMap<>();
        this.outByAp = new HashMap<>();
        this.inByAp = new HashMap<>();
        this.capRemain = new HashMap<>();
    }

    /**
     * Expande los vuelos programados en instancias concretas para una ventana temporal.
     * Cada vuelo se replica para cada día en la ventana.
     *
     * @param tStart Inicio de la ventana (UTC)
     * @param tEnd Fin de la ventana (UTC)
     */
    public void expandirInstancias(Instant tStart, Instant tEnd) {
        log.info("Expandiendo instancias de vuelos para ventana {} - {}", tStart, tEnd);

        instancias.clear();
        byId.clear();
        outByAp.clear();
        inByAp.clear();
        capRemain.clear();

        // Calcular rango de días
        LocalDate startDate = tStart.atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = tEnd.atZone(ZoneId.of("UTC")).toLocalDate();
        long numDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Para cada vuelo programado
        for (T06VueloProgramado vuelo : vuelosProgramados) {
            expandirVuelo(vuelo, startDate, numDays, tStart, tEnd);
        }

        // Ordenar listas por tiempo
        for (List<FlightInstance> lista : outByAp.values()) {
            lista.sort(Comparator.comparing(FlightInstance::getDepUtc));
        }
        for (List<FlightInstance> lista : inByAp.values()) {
            lista.sort(Comparator.comparing(FlightInstance::getArrUtc));
        }

        log.info("Instancias generadas: {} vuelos", instancias.size());
        log.info("Aeropuertos con salidas: {}", outByAp.size());
        log.info("Aeropuertos con llegadas: {}", inByAp.size());
    }

    /**
     * Expande un vuelo programado en múltiples instancias (una por día).
     */
    private void expandirVuelo(T06VueloProgramado vuelo, LocalDate startDate, long numDays,
                                Instant tStart, Instant tEnd) {

        String codigoOrigen = vuelo.getT01Idaeropuertoorigen().getT01Codigoicao();
        String codigoDestino = vuelo.getT01Idaeropuertodestino().getT01Codigoicao();

        // Obtener zona horaria del aeropuerto origen
        ZoneId zonaOrigen = obtenerZonaHoraria(codigoOrigen);

        // Para cada día en la ventana
        for (long dayOffset = 0; dayOffset < numDays; dayOffset++) {
            LocalDate fecha = startDate.plusDays(dayOffset);

            // Construir Instant de salida
            LocalDateTime salidaLocal = fecha.atTime(
                vuelo.getT06Fechasalida().atZone(zonaOrigen).toLocalTime()
            );
            Instant salidaUtc = salidaLocal.atZone(zonaOrigen).toInstant();

            // Construir Instant de llegada
            LocalDateTime llegadaLocal = fecha.atTime(
                vuelo.getT06Fechallegada().atZone(zonaOrigen).toLocalTime()
            );
            Instant llegadaUtc = llegadaLocal.atZone(zonaOrigen).toInstant();

            // Ajustar si la llegada es al día siguiente
            if (llegadaUtc.isBefore(salidaUtc)) {
                llegadaUtc = llegadaUtc.plus(1, ChronoUnit.DAYS);
            }

            // Verificar que esté dentro de la ventana
            if (salidaUtc.isBefore(tStart) || salidaUtc.isAfter(tEnd)) {
                continue;
            }

            // Crear instancia
            String instanceId = FlightInstance.buildInstanceId(codigoOrigen, codigoDestino, salidaUtc);

            FlightInstance instance = FlightInstance.builder()
                .instanceId(instanceId)
                .vueloId(vuelo.getId())
                .origen(codigoOrigen)
                .destino(codigoDestino)
                .capacidad(vuelo.getT06Capacidadtotal() != null ? vuelo.getT06Capacidadtotal() : 0)
                .depUtc(salidaUtc)
                .arrUtc(llegadaUtc)
                .ocupados(vuelo.getT06Ocupacionactual() != null ? vuelo.getT06Ocupacionactual() : 0)
                .cancelado("CANCELADO".equals(vuelo.getT06Estado()))
                .build();

            // Agregar a estructuras
            instancias.add(instance);
            byId.put(instanceId, instance);
            capRemain.put(instanceId, instance.remaining());

            // Agregar a índices por aeropuerto
            outByAp.computeIfAbsent(codigoOrigen, k -> new ArrayList<>()).add(instance);
            inByAp.computeIfAbsent(codigoDestino, k -> new ArrayList<>()).add(instance);
        }
    }

    /**
     * Obtiene la zona horaria de un aeropuerto por su código ICAO.
     */
    private ZoneId obtenerZonaHoraria(String codigoIcao) {
        T01Aeropuerto aeropuerto = aeropuertosPorCodigo.get(codigoIcao);
        if (aeropuerto != null && aeropuerto.getT08Idciudad() != null) {
            String zonaHoraria = aeropuerto.getT08Idciudad().getT08Zonahoraria();
            if (zonaHoraria != null && !zonaHoraria.isEmpty()) {
                try {
                    return ZoneId.of(zonaHoraria);
                } catch (Exception e) {
                    log.warn("Zona horaria inválida para {}: {}", codigoIcao, zonaHoraria);
                }
            }
        }
        // Default UTC
        return ZoneId.of("UTC");
    }

    // ========== Consultas ==========

    /**
     * Obtiene una instancia de vuelo por su ID.
     */
    public FlightInstance getById(String instanceId) {
        return byId.get(instanceId);
    }

    /**
     * Obtiene todos los vuelos salientes de un aeropuerto (ordenados por hora de salida).
     */
    public List<FlightInstance> getSalidasDesde(String codigoIcao) {
        return outByAp.getOrDefault(codigoIcao, Collections.emptyList());
    }

    /**
     * Obtiene todos los vuelos entrantes a un aeropuerto (ordenados por hora de llegada).
     */
    public List<FlightInstance> getLlegadasA(String codigoIcao) {
        return inByAp.getOrDefault(codigoIcao, Collections.emptyList());
    }

    /**
     * Obtiene vuelos salientes desde un aeropuerto después de un tiempo dado.
     *
     * @param codigoIcao Código ICAO del aeropuerto
     * @param despuesDe Tiempo mínimo de salida
     * @return Lista de vuelos que salen después del tiempo dado
     */
    public List<FlightInstance> getSalidasDesdeDespuesDe(String codigoIcao, Instant despuesDe) {
        List<FlightInstance> salidas = getSalidasDesde(codigoIcao);
        return salidas.stream()
            .filter(f -> !f.getDepUtc().isBefore(despuesDe))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene vuelos entrantes a un aeropuerto en una ventana de tiempo.
     */
    public List<FlightInstance> getLlegadasEntre(String codigoIcao, Instant desde, Instant hasta) {
        List<FlightInstance> llegadas = getLlegadasA(codigoIcao);
        return llegadas.stream()
            .filter(f -> !f.getArrUtc().isBefore(desde) && !f.getArrUtc().isAfter(hasta))
            .collect(Collectors.toList());
    }

    /**
     * Obtiene vuelos salientes de un aeropuerto en una ventana de tiempo.
     */
    public List<FlightInstance> getSalidasEntre(String codigoIcao, Instant desde, Instant hasta) {
        List<FlightInstance> salidas = getSalidasDesde(codigoIcao);
        return salidas.stream()
            .filter(f -> !f.getDepUtc().isBefore(desde) && !f.getDepUtc().isAfter(hasta))
            .collect(Collectors.toList());
    }

    // ========== Gestión de Capacidades ==========

    /**
     * Obtiene la capacidad remanente de un vuelo.
     */
    public int getCapacidadRemanente(String instanceId) {
        return capRemain.getOrDefault(instanceId, 0);
    }

    /**
     * Actualiza la capacidad remanente de un vuelo.
     */
    public void setCapacidadRemanente(String instanceId, int nuevaCapacidad) {
        capRemain.put(instanceId, nuevaCapacidad);

        // Actualizar también en la instancia
        FlightInstance flight = byId.get(instanceId);
        if (flight != null) {
            flight.setOcupados(flight.getCapacidad() - nuevaCapacidad);
        }
    }

    /**
     * Carga paquetes en un vuelo (reduce capacidad remanente).
     *
     * @param instanceId ID de la instancia
     * @param cantidad Cantidad a cargar
     * @return true si se pudo cargar, false si no hay capacidad
     */
    public boolean cargarVuelo(String instanceId, int cantidad) {
        FlightInstance flight = byId.get(instanceId);
        if (flight == null || flight.isCancelado()) {
            return false;
        }

        int capActual = capRemain.getOrDefault(instanceId, 0);
        if (capActual < cantidad) {
            return false;
        }

        capRemain.put(instanceId, capActual - cantidad);
        flight.load(cantidad);
        return true;
    }

    /**
     * Descarga paquetes de un vuelo (aumenta capacidad remanente).
     */
    public void descargarVuelo(String instanceId, int cantidad) {
        FlightInstance flight = byId.get(instanceId);
        if (flight == null) {
            return;
        }

        int capActual = capRemain.getOrDefault(instanceId, 0);
        capRemain.put(instanceId, capActual + cantidad);
        flight.unload(cantidad);
    }

    /**
     * Cancela un vuelo y libera su capacidad.
     */
    public void cancelarVuelo(String instanceId) {
        FlightInstance flight = byId.get(instanceId);
        if (flight != null) {
            flight.cancel();
            capRemain.put(instanceId, 0);
        }
    }

    // ========== Utilidades ==========

    /**
     * Obtiene un aeropuerto por su código ICAO.
     */
    public T01Aeropuerto getAeropuerto(String codigoIcao) {
        return aeropuertosPorCodigo.get(codigoIcao);
    }

    /**
     * Obtiene todos los códigos ICAO de aeropuertos con vuelos salientes.
     */
    public Set<String> getAeropuertosConSalidas() {
        return outByAp.keySet();
    }

    /**
     * Obtiene el número total de instancias de vuelo.
     */
    public int getNumInstancias() {
        return instancias.size();
    }

    /**
     * Obtiene todas las instancias de vuelo.
     */
    public List<FlightInstance> getTodasLasInstancias() {
        return new ArrayList<>(instancias);
    }

    /**
     * Reinicia las capacidades de todos los vuelos a sus valores originales.
     */
    public void reiniciarCapacidades() {
        for (FlightInstance flight : instancias) {
            flight.setOcupados(0);
            capRemain.put(flight.getInstanceId(), flight.getCapacidad());
        }
    }

    /**
     * Obtiene estadísticas del grafo.
     */
    public Map<String, Object> getEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstancias", instancias.size());
        stats.put("aeropuertosConSalidas", outByAp.size());
        stats.put("aeropuertosConLlegadas", inByAp.size());

        int capacidadTotal = instancias.stream()
            .mapToInt(FlightInstance::getCapacidad)
            .sum();
        int ocupacionTotal = instancias.stream()
            .mapToInt(FlightInstance::getOcupados)
            .sum();

        stats.put("capacidadTotal", capacidadTotal);
        stats.put("ocupacionTotal", ocupacionTotal);
        stats.put("ocupacionPct", capacidadTotal > 0 ? (ocupacionTotal * 100.0 / capacidadTotal) : 0.0);

        long cancelados = instancias.stream()
            .filter(FlightInstance::isCancelado)
            .count();
        stats.put("vuelosCancelados", cancelados);

        return stats;
    }
}
