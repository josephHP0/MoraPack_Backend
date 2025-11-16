package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VueloProgramadoRepository extends JpaRepository<T04VueloProgramado, Integer> {

    @Query("SELECT v FROM T04VueloProgramado v WHERE v.t04FechaSalida BETWEEN :fechaInicio AND :fechaFin " +
           "AND v.t04Estado != 'CANCELADO'")
    List<T04VueloProgramado> findVuelosDisponibles(@Param("fechaInicio") Instant fechaInicio,
                                                     @Param("fechaFin") Instant fechaFin);

    @Query("SELECT v FROM T04VueloProgramado v WHERE v.t01IdAeropuertoOrigen.id = :origenId " +
           "AND v.t01IdAeropuertoDestino.id = :destinoId AND v.t04FechaSalida >= :fechaMinima " +
           "AND v.t04Estado != 'CANCELADO'")
    List<T04VueloProgramado> findVuelosEntreAeropuertos(@Param("origenId") Integer origenId,
                                                          @Param("destinoId") Integer destinoId,
                                                          @Param("fechaMinima") Instant fechaMinima);

    /**
     * Obtiene todos los vuelos plantilla (del día base) sin filtro de fechas.
     * Los vuelos en la BD representan 1 día de operaciones que se repite.
     * Este método los obtiene todos para posteriormente expandirlos a múltiples días.
     *
     * IMPORTANTE: Usa JOIN FETCH para evitar N+1 queries cuando se accede a aeropuertos y aviones.
     * Esto carga EAGER las relaciones necesarias en una sola consulta SQL con JOINs.
     */
    @Query("SELECT DISTINCT v FROM T04VueloProgramado v " +
           "LEFT JOIN FETCH v.t01IdAeropuertoOrigen " +
           "LEFT JOIN FETCH v.t01IdAeropuertoDestino " +
           "LEFT JOIN FETCH v.t11IdAvion " +
           "ORDER BY v.t04FechaSalida")
    List<T04VueloProgramado> findAllVuelosPlantilla();
}
