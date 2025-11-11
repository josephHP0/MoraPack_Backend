package com.morapack.repository;

import com.morapack.model.T10SimulacionSemanal;
import com.morapack.model.T13MetricaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de métricas diarias.
 */
@Repository
public interface T13MetricaDiariaRepository extends JpaRepository<T13MetricaDiaria, Integer> {

    /**
     * Encuentra métricas por simulación.
     */
    List<T13MetricaDiaria> findBySimulacionSemanal(T10SimulacionSemanal simulacion);

    /**
     * Encuentra métricas por simulación ordenadas por fecha.
     */
    List<T13MetricaDiaria> findBySimulacionSemanalOrderByFechaAsc(T10SimulacionSemanal simulacion);

    /**
     * Encuentra métrica de una simulación en una fecha específica.
     */
    Optional<T13MetricaDiaria> findBySimulacionSemanalAndFecha(T10SimulacionSemanal simulacion, LocalDate fecha);

    /**
     * Encuentra métricas por ID de simulación.
     */
    List<T13MetricaDiaria> findBySimulacionSemanalIdOrderByFechaAsc(Integer simulacionId);
}
