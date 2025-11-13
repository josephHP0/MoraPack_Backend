package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T07Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<T07Ruta, Integer> {

    @Query("SELECT r FROM T07Ruta r WHERE r.t01IdAeroOrigen.id = :origenId " +
           "AND r.t01IdAeroDestino.id = :destinoId ORDER BY r.t07Sla ASC")
    List<T07Ruta> findRutasPosibles(@Param("origenId") Integer origenId,
                                     @Param("destinoId") Integer destinoId);
}
