package com.morapack.nucleo;

import com.morapack.adapter.AeropuertoAdapter;
import com.morapack.adapter.VueloProgramadoAdapter;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T06VueloProgramado;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Grafo de vuelos que ahora distingue entre:
 * - Plantillas diarias (Vuelo) leídas de vuelos.txt
 * - Instancias diarias (FlightInstance) generadas para un rango [tStart..tEnd]
 */
public class GrafoVuelos {

    // ==== Arista para heurísticas "clásicas" (compatibilidad) ====
    public static class Arista {
        public final String a;     // origen
        public final String b;     // destino
        public final double horas; // duración
        public final int vueloId;  // id plantilla
        public Arista(String a, String b, double h, int id) {
            this.a = a; this.b = b; this.horas = h; this.vueloId = id;
        }
    }

    // ==== NUEVO: Instancia diaria (slot) con tiempos concretos ====
    public static class FlightInstance {
        /** Id única de instancia, p.ej. "EBCI-EDDI-1037-20251101" */
        public final String instanceId;

        /** Origen/Destino (IATA) y capacidad de la instancia diaria. */
        public final String origen;
        public final String destino;
        public final int capacidad;

        /** Salida y llegada en UTC (el Grafo expande usando zonas de cada aeropuerto). */
        public final java.time.Instant arrUtc;
        public final java.time.Instant depUtc;

        // === NUEVO: estado vivo de la instancia ===
        private int ocupados = 0;
        private boolean cancelado = false;

        public FlightInstance(String instanceId,
                              String origen,
                              String destino,
                              java.time.Instant arrUtc,
                              java.time.Instant depUtc,
                              int capacidad) {
            this.instanceId = java.util.Objects.requireNonNull(instanceId, "instanceId");
            this.origen = java.util.Objects.requireNonNull(origen, "origen");
            this.destino = java.util.Objects.requireNonNull(destino, "destino");
            this.arrUtc = java.util.Objects.requireNonNull(arrUtc, "salidaUtc");
            this.depUtc = java.util.Objects.requireNonNull(depUtc, "llegadaUtc");
            this.capacidad = Math.max(0, capacidad);
        }

        /** Capacidad restante. */
        public int remaining() {
            return Math.max(0, capacidad - ocupados);
        }

        /** ¿Puede cargar 'qty' paquetes? */
        public boolean canLoad(int qty) {
            return !cancelado && qty > 0 && remaining() >= qty;
        }

        /** Marca carga a bordo (sin side-effects externos). Lanza si no hay capacidad. */
        public void load(int qty) {
            if (!canLoad(qty)) {
                throw new IllegalStateException("No hay capacidad en " + instanceId + " para " + qty + " paquetes");
            }
            ocupados += qty;
        }

        /** Descarga “virtual” (por sim). */
        public void unload(int qty) {
            if (qty <= 0) return;
            ocupados = Math.max(0, ocupados - qty);
        }

        /** Cancela esta instancia (para aplicar archivo de cancelaciones). */
        public void cancel() {
            this.cancelado = true;
        }

        public boolean isCancelled() {
            return cancelado;
        }

        public int getOcupados() {
            return ocupados;
        }

        public String legKey() {
            return origen + "-" + destino;
        }

        @Override
        public String toString() {
            return instanceId + " [" + origen + "→" + destino + "] cap=" + capacidad + " used=" + ocupados + (cancelado ? " CANCELLED" : "");
        }

        @Override
        public int hashCode() {
            return instanceId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FlightInstance other)) return false;
            return this.instanceId.equals(other.instanceId);
        }
    }
    // ==== Datos base ====
    private final List<T06VueloProgramado> vuelos = new ArrayList<>();
    private final Map<String, List<Arista>> ady = new HashMap<>();

    // ==== Constructor con vuelos programados ====
    public GrafoVuelos(List<T06VueloProgramado> vuelos) {
        this.vuelos.addAll(vuelos);
        for (T06VueloProgramado v : vuelos) {
            VueloProgramadoAdapter adapter = new VueloProgramadoAdapter(v);
            String origen = adapter.getOrigen();
            ady.computeIfAbsent(origen, k -> new ArrayList<>())
               .add(new Arista(origen, adapter.getDestino(), 
                             adapter.getDurationMinutes() / 60.0, v.getId()));
        }
    }

    public List<T06VueloProgramado> getVuelos() {
        return vuelos;
    }

    public List<Arista> aristasDesde(String origen) {
        return ady.getOrDefault(origen, Collections.emptyList());
    }

    // ==== Expansión de vuelos programados a instancias para la ventana temporal ====
    public List<FlightInstance> expandirSlots(Instant tStart, Instant tEnd, 
                                            Map<String, T01Aeropuerto> aeropuertos) {
        List<FlightInstance> out = new ArrayList<>();
        for (T06VueloProgramado vuelo : vuelos) {
            VueloProgramadoAdapter vueloAdapter = new VueloProgramadoAdapter(vuelo);
            
            T01Aeropuerto aOri = vuelo.getT01Idaeropuertoorigen();
            T01Aeropuerto aDes = vuelo.getT01Idaeropuertodestino();
            if (aOri == null || aDes == null) continue;

            // Determinar zona horaria basada en el código ICAO del aeropuerto
            ZoneId zOrigen = getZonaFromAirport(aOri.getT01Codigoicao());
            ZoneId zDestino = getZonaFromAirport(aDes.getT01Codigoicao());

            // Rango de fechas en la zona de origen
            LocalDate startDate = LocalDateTime.ofInstant(tStart, zOrigen).toLocalDate();
            LocalDate endDate = LocalDateTime.ofInstant(tEnd, zOrigen).toLocalDate();

            // Para cada vuelo programado, generamos una instancia
            if (vuelo.getT06Fechasalida() != null && vuelo.getT06Fechallegada() != null) {
                LocalDateTime salida = LocalDateTime.ofInstant(vuelo.getT06Fechasalida(), zOrigen);
                DateTimeFormatter ymd = DateTimeFormatter.BASIC_ISO_DATE;

                String hhmm = String.format("%02d%02d", salida.getHour(), salida.getMinute());
                String instanceId = aOri.getT01Codigoicao() + "-" + 
                                  aDes.getT01Codigoicao() + "-" + 
                                  hhmm + "-" + salida.toLocalDate().format(ymd);

                out.add(new FlightInstance(
                        instanceId,
                        aOri.getT01Codigoicao(),
                        aDes.getT01Codigoicao(),
                        vuelo.getT06Fechasalida(),
                        vuelo.getT06Fechallegada(),
                        vuelo.getT06Capacidadtotal() != null ? vuelo.getT06Capacidadtotal() : 0
                ));
            }
        }
        // Se podría indexar por origen para consultas rápidas; lo dejamos simple aquí
        return out;
    }

    // ==== NUEVO: utilidad para formar instanceId a partir de día/hora/O-D ====
    public static String buildInstanceId(String origen, String destino, int hora, int minuto, LocalDate fecha) {
        String hhmm = String.format("%02d%02d", hora, minuto);
        return origen + "-" + destino + "-" + hhmm + "-" + fecha.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * Determina la zona horaria basada en el código ICAO del aeropuerto.
     */
    private ZoneId getZonaFromAirport(String icaoCode) {
        // Adaptar según tus aeropuertos
        return switch (icaoCode) {
            case "SPIM" -> ZoneId.of("America/Lima");       // Lima, Peru
            case "SEGU" -> ZoneId.of("America/Guayaquil");  // Guayaquil, Ecuador
            case "SKBO" -> ZoneId.of("America/Bogota");     // Bogotá, Colombia
            case "SCEL" -> ZoneId.of("America/Santiago");   // Santiago, Chile
            case "SAEZ" -> ZoneId.of("America/Argentina/Buenos_Aires"); // Buenos Aires
            case "SBGR" -> ZoneId.of("America/Sao_Paulo");  // São Paulo, Brasil
            default -> ZoneId.of("UTC");                    // Default UTC
        };
    }
}
