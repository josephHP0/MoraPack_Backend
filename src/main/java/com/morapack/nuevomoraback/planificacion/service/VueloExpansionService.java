package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T01Aeropuerto;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.domain.T06Avion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para expandir vuelos plantilla a lo largo de múltiples días.
 *
 * Los vuelos en la base de datos representan UN día de operaciones (2866 vuelos).
 * Este servicio replica estos vuelos para cada día del periodo de simulación,
 * manteniendo las horas de salida/llegada pero ajustando las fechas.
 */
@Slf4j
@Service
public class VueloExpansionService {

    /**
     * Expande los vuelos plantilla para todo el rango de fechas de la simulación.
     *
     * IMPORTANTE: Expande con buffer adicional:
     * - 1 día ANTES del inicio (para pedidos que necesitan vuelos inmediatos)
     * - 7 días DESPUÉS del fin (para cubrir SLA de 72 horas)
     *
     * @param vuelosPlantilla Vuelos base (1 día de operaciones)
     * @param fechaInicio Inicio de la simulación
     * @param fechaFin Fin de la simulación
     * @return Lista de vuelos virtuales expandidos para todos los días
     */
    public List<VueloVirtual> expandirVuelos(List<T04VueloProgramado> vuelosPlantilla,
                                              Instant fechaInicio,
                                              Instant fechaFin) {

        if (vuelosPlantilla.isEmpty()) {
            log.warn("No hay vuelos plantilla para expandir");
            return new ArrayList<>();
        }

        // Determinar el día base de los vuelos plantilla (usar el primer vuelo)
        Instant fechaBaseSalida = vuelosPlantilla.get(0).getT04FechaSalida();
        LocalDate diaBase = fechaBaseSalida.atZone(ZoneId.of("UTC")).toLocalDate();

        // Calcular el rango de días de la simulación CON BUFFER
        // Buffer: 1 día antes + 7 días después (para cumplir SLA de hasta 72h)
        LocalDate inicioSimulacion = fechaInicio.atZone(ZoneId.of("UTC")).toLocalDate().minusDays(1);
        LocalDate finSimulacion = fechaFin.atZone(ZoneId.of("UTC")).toLocalDate().plusDays(7);

        log.info("Expandiendo {} vuelos plantilla del día base {} para el periodo {} a {} (con buffer)",
                 vuelosPlantilla.size(), diaBase, inicioSimulacion, finSimulacion);

        List<VueloVirtual> vuelosExpandidos = new ArrayList<>();

        // Para cada día de la simulación
        LocalDate diaActual = inicioSimulacion;
        while (!diaActual.isAfter(finSimulacion)) {

            // Calcular la diferencia en días entre el día actual y el día base
            long diasDiferencia = Duration.between(
                diaBase.atStartOfDay(ZoneId.of("UTC")),
                diaActual.atStartOfDay(ZoneId.of("UTC"))
            ).toDays();

            // Expandir cada vuelo plantilla para este día
            for (T04VueloProgramado plantilla : vuelosPlantilla) {
                VueloVirtual vueloVirtual = crearVueloVirtual(plantilla, diasDiferencia);
                vuelosExpandidos.add(vueloVirtual);
            }

            diaActual = diaActual.plusDays(1);
        }

        log.info("Expansión completada: {} vuelos virtuales generados para {} días",
                 vuelosExpandidos.size(),
                 Duration.between(inicioSimulacion.atStartOfDay(ZoneId.of("UTC")),
                                  finSimulacion.atStartOfDay(ZoneId.of("UTC"))).toDays() + 1);

        return vuelosExpandidos;
    }

    /**
     * Crea un vuelo virtual basado en una plantilla, ajustando las fechas.
     *
     * @param plantilla Vuelo plantilla original
     * @param diasDiferencia Días a sumar a las fechas de la plantilla
     * @return Vuelo virtual con fechas ajustadas
     */
    private VueloVirtual crearVueloVirtual(T04VueloProgramado plantilla, long diasDiferencia) {
        VueloVirtual vuelo = new VueloVirtual();

        // Copiar datos base de la plantilla
        vuelo.setPlantillaId(plantilla.getId());
        vuelo.setOrigen(plantilla.getT01IdAeropuertoOrigen());
        vuelo.setDestino(plantilla.getT01IdAeropuertoDestino());
        vuelo.setAvion(plantilla.getT11IdAvion());
        vuelo.setCapacidadTotal(plantilla.getT04CapacidadTotal());
        vuelo.setOcupacionTotal(0); // Los vuelos virtuales empiezan vacíos
        vuelo.setEstado(plantilla.getT04Estado());
        vuelo.setEstadoCapacidad(plantilla.getT04EstadoCapacidad());

        // Ajustar fechas sumando los días de diferencia
        Instant nuevaSalida = plantilla.getT04FechaSalida().plus(Duration.ofDays(diasDiferencia));
        Instant nuevaLlegada = plantilla.getT04FechaLlegada().plus(Duration.ofDays(diasDiferencia));

        vuelo.setFechaSalida(nuevaSalida);
        vuelo.setFechaLlegada(nuevaLlegada);

        return vuelo;
    }

    /**
     * Clase interna que representa un vuelo virtual (no persistido en BD).
     * Mantiene referencia al vuelo plantilla original y las fechas ajustadas.
     */
    @Slf4j
    public static class VueloVirtual {
        private Integer plantillaId; // ID del vuelo plantilla original
        private T01Aeropuerto origen;
        private T01Aeropuerto destino;
        private T06Avion avion;
        private Instant fechaSalida;
        private Instant fechaLlegada;
        private Integer capacidadTotal;
        private Integer ocupacionTotal;
        private String estado;
        private String estadoCapacidad;

        // Getters y setters
        public Integer getPlantillaId() { return plantillaId; }
        public void setPlantillaId(Integer plantillaId) { this.plantillaId = plantillaId; }

        public T01Aeropuerto getOrigen() { return origen; }
        public void setOrigen(T01Aeropuerto origen) { this.origen = origen; }

        public T01Aeropuerto getDestino() { return destino; }
        public void setDestino(T01Aeropuerto destino) { this.destino = destino; }

        public T06Avion getAvion() { return avion; }
        public void setAvion(T06Avion avion) { this.avion = avion; }

        public Instant getFechaSalida() { return fechaSalida; }
        public void setFechaSalida(Instant fechaSalida) { this.fechaSalida = fechaSalida; }

        public Instant getFechaLlegada() { return fechaLlegada; }
        public void setFechaLlegada(Instant fechaLlegada) { this.fechaLlegada = fechaLlegada; }

        public Integer getCapacidadTotal() { return capacidadTotal; }
        public void setCapacidadTotal(Integer capacidadTotal) { this.capacidadTotal = capacidadTotal; }

        public Integer getOcupacionTotal() { return ocupacionTotal; }
        public void setOcupacionTotal(Integer ocupacionTotal) { this.ocupacionTotal = ocupacionTotal; }

        public Integer getCapacidadDisponible() {
            return capacidadTotal - ocupacionTotal;
        }

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public String getEstadoCapacidad() { return estadoCapacidad; }
        public void setEstadoCapacidad(String estadoCapacidad) { this.estadoCapacidad = estadoCapacidad; }

        /**
         * Convierte este vuelo virtual a una entidad T04VueloProgramado para persistencia.
         * NOTA: Esto crea una entidad con el ID de la plantilla original.
         */
        public T04VueloProgramado toEntity() {
            T04VueloProgramado entity = new T04VueloProgramado();
            entity.setId(plantillaId);
            entity.setT01IdAeropuertoOrigen(origen);
            entity.setT01IdAeropuertoDestino(destino);
            entity.setT11IdAvion(avion);
            entity.setT04FechaSalida(fechaSalida);
            entity.setT04FechaLlegada(fechaLlegada);
            entity.setT04CapacidadTotal(capacidadTotal);
            entity.setT04OcupacionTotal(ocupacionTotal);
            entity.setT04Estado(estado);
            entity.setT04EstadoCapacidad(estadoCapacidad);
            return entity;
        }
    }
}
