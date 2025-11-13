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
}
