package com.morapack.nuevomoraback.planificacion.repository;

import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface T08RutaPlaneadaRepository extends JpaRepository<T08RutaPlaneada, Integer> {

    @Query("SELECT r FROM T08RutaPlaneada r WHERE r.tipoSimulacion = :tipo")
    List<T08RutaPlaneada> findByTipoSimulacion(@Param("tipo") T08RutaPlaneada.TipoSimulacion tipo);

    @Query("SELECT r FROM T08RutaPlaneada r WHERE r.estado = :estado")
    List<T08RutaPlaneada> findByEstado(@Param("estado") T08RutaPlaneada.EstadoRuta estado);
}
