package com.morapack.nuevomoraback.planificacion.repository;

import com.morapack.nuevomoraback.planificacion.domain.T12CancelacionVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface T12CancelacionVueloRepository extends JpaRepository<T12CancelacionVuelo, Integer> {

    @Query("SELECT c FROM T12CancelacionVuelo c WHERE c.fechaCancelacion BETWEEN :fechaInicio AND :fechaFin")
    List<T12CancelacionVuelo> findCancelacionesEnRango(@Param("fechaInicio") Instant fechaInicio,
                                                         @Param("fechaFin") Instant fechaFin);

    @Query("SELECT c FROM T12CancelacionVuelo c WHERE c.vueloProgramado.id = :vueloId")
    List<T12CancelacionVuelo> findByVueloId(@Param("vueloId") Integer vueloId);
}
