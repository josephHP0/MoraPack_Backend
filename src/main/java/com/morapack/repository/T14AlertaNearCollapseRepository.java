package com.morapack.repository;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T10SimulacionSemanal;
import com.morapack.model.T14AlertaNearCollapse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestión de alertas de near-collapse.
 */
@Repository
public interface T14AlertaNearCollapseRepository extends JpaRepository<T14AlertaNearCollapse, Integer> {

    /**
     * Encuentra alertas por simulación.
     */
    List<T14AlertaNearCollapse> findBySimulacionSemanal(T10SimulacionSemanal simulacion);

    /**
     * Encuentra alertas por simulación ordenadas por fecha.
     */
    List<T14AlertaNearCollapse> findBySimulacionSemanalOrderByFechaHoraDesc(T10SimulacionSemanal simulacion);

    /**
     * Encuentra alertas por aeropuerto.
     */
    List<T14AlertaNearCollapse> findByAeropuerto(T01Aeropuerto aeropuerto);

    /**
     * Encuentra alertas por severidad.
     */
    List<T14AlertaNearCollapse> findBySeveridad(String severidad);

    /**
     * Encuentra alertas no resueltas de una simulación.
     */
    List<T14AlertaNearCollapse> findBySimulacionSemanalAndResueltoFalseOrderByFechaHoraDesc(T10SimulacionSemanal simulacion);

    /**
     * Encuentra alertas críticas no resueltas.
     */
    List<T14AlertaNearCollapse> findBySeveridadAndResueltoFalse(String severidad);
}
