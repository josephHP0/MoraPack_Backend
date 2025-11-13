package com.morapack.nuevomoraback.planificacion.repository;

import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import com.morapack.nuevomoraback.planificacion.domain.T10ResultadoSimulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface T10ResultadoSimulacionRepository extends JpaRepository<T10ResultadoSimulacion, Integer> {

    @Query("SELECT r FROM T10ResultadoSimulacion r WHERE r.tipoSimulacion = :tipo ORDER BY r.fechaEjecucion DESC")
    List<T10ResultadoSimulacion> findByTipoSimulacionOrderByFechaDesc(@Param("tipo") T08RutaPlaneada.TipoSimulacion tipo);

    Optional<T10ResultadoSimulacion> findTopByTipoSimulacionOrderByFechaEjecucionDesc(T08RutaPlaneada.TipoSimulacion tipoSimulacion);
}
