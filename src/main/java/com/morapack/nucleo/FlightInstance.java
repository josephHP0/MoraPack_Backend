package com.morapack.nucleo;

import lombok.*;

import java.time.Instant;

/**
 * Represent una instancia concreta de un vuelo en un día/hora específicos.
 * A diferencia de la plantilla de vuelo (T06VueloProgramado), esta clase
 * incluye el estado mutable para simulación (capacidad remanente, cancelación).
 *
 * Basada en FlightInstance del proyecto de referencia, adaptada para trabajar
 * con las entidades JPA de MoraPack.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FlightInstance {

    /** ID único de la instancia (formato: "ORIGEN-DESTINO-HHmm-YYYYMMDD") */
    private String instanceId;

    /** ID de la entidad T06VueloProgramado en la BD */
    private Integer vueloId;

    /** Código ICAO del aeropuerto de origen */
    private String origen;

    /** Código ICAO del aeropuerto de destino */
    private String destino;

    /** Capacidad total del vuelo */
    private int capacidad;

    /** Hora de salida en UTC */
    private Instant depUtc;

    /** Hora de llegada en UTC */
    private Instant arrUtc;

    /** Cantidad de paquetes ya ocupados (mutable durante simulación) */
    private int ocupados;

    /** Si el vuelo está cancelado */
    private boolean cancelado;

    // ========== Métodos de Utilidad ==========

    /**
     * Retorna la capacidad remanente del vuelo.
     *
     * @return Número de paquetes que aún pueden cargarse
     */
    public int remaining() {
        return Math.max(0, capacidad - ocupados);
    }

    /**
     * Verifica si el vuelo puede cargar una cantidad de paquetes.
     *
     * @param qty Cantidad a cargar
     * @return true si hay capacidad suficiente y el vuelo no está cancelado
     */
    public boolean canLoad(int qty) {
        return !cancelado && remaining() >= qty;
    }

    /**
     * Carga una cantidad de paquetes en el vuelo.
     *
     * @param qty Cantidad a cargar
     * @throws IllegalArgumentException si no hay capacidad o vuelo cancelado
     */
    public void load(int qty) {
        if (cancelado) {
            throw new IllegalArgumentException(
                "No se puede cargar en vuelo cancelado: " + instanceId
            );
        }
        if (remaining() < qty) {
            throw new IllegalArgumentException(
                String.format("Capacidad insuficiente en %s: disponible=%d, solicitado=%d",
                    instanceId, remaining(), qty)
            );
        }
        ocupados += qty;
    }

    /**
     * Descarga una cantidad de paquetes del vuelo.
     *
     * @param qty Cantidad a descargar
     */
    public void unload(int qty) {
        ocupados = Math.max(0, ocupados - qty);
    }

    /**
     * Marca el vuelo como cancelado.
     */
    public void cancel() {
        this.cancelado = true;
    }

    /**
     * Calcula el porcentaje de ocupación del vuelo.
     *
     * @return Ocupación como porcentaje (0-100)
     */
    public double getOcupacionPct() {
        return capacidad > 0 ? (ocupados * 100.0 / capacidad) : 0.0;
    }

    /**
     * Calcula la duración del vuelo en horas.
     *
     * @return Duración en horas
     */
    public double getDuracionHoras() {
        if (depUtc == null || arrUtc == null) {
            return 0.0;
        }
        long seconds = arrUtc.getEpochSecond() - depUtc.getEpochSecond();
        return seconds / 3600.0;
    }

    /**
     * Crea una copia profunda de la instancia (para simulación).
     *
     * @return Nueva instancia con los mismos valores
     */
    public FlightInstance copy() {
        return FlightInstance.builder()
            .instanceId(this.instanceId)
            .vueloId(this.vueloId)
            .origen(this.origen)
            .destino(this.destino)
            .capacidad(this.capacidad)
            .depUtc(this.depUtc)
            .arrUtc(this.arrUtc)
            .ocupados(this.ocupados)
            .cancelado(this.cancelado)
            .build();
    }

    /**
     * Crea un ID de instancia estándar.
     *
     * @param origen Código del aeropuerto origen
     * @param destino Código del aeropuerto destino
     * @param depUtc Hora de salida UTC
     * @return ID único de instancia
     */
    public static String buildInstanceId(String origen, String destino, Instant depUtc) {
        // Formato: ORIG-DEST-HHmm-YYYYMMDD
        java.time.ZonedDateTime zdt = depUtc.atZone(java.time.ZoneId.of("UTC"));
        return String.format("%s-%s-%02d%02d-%04d%02d%02d",
            origen, destino,
            zdt.getHour(), zdt.getMinute(),
            zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth()
        );
    }
}
