package com.morapack.repository;

import com.morapack.model.T10SimulacionSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio para gestión de simulaciones semanales.
 */
@Repository
public interface T10SimulacionSemanalRepository extends JpaRepository<T10SimulacionSemanal, Integer> {

    /**
     * Encuentra simulaciones por estado ordenadas por fecha de creación descendente.
     */
    List<T10SimulacionSemanal> findByEstadoOrderByFechaCreacionDesc(String estado);

    /**
     * Encuentra todas las simulaciones ordenadas por fecha de creación descendente.
     */
    List<T10SimulacionSemanal> findAllByOrderByFechaCreacionDesc();

    /**
     * Encuentra simulaciones en un rango de fechas.
     */
    List<T10SimulacionSemanal> findByFechaInicioBetween(Instant inicio, Instant fin);

    /**
     * Encuentra la simulación más reciente.
     */
    T10SimulacionSemanal findFirstByOrderByFechaCreacionDesc();
}
