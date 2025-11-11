package com.morapack.repository;

import com.morapack.model.T13MetricaDiaria;
import com.morapack.model.T15TopAeropuertoWps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestión del ranking de aeropuertos por WPS.
 */
@Repository
public interface T15TopAeropuertoWpsRepository extends JpaRepository<T15TopAeropuertoWps, Integer> {

    /**
     * Encuentra el top por métrica diaria ordenado por ranking.
     */
    List<T15TopAeropuertoWps> findByMetricaDiariaOrderByRankingAsc(T13MetricaDiaria metricaDiaria);

    /**
     * Encuentra el top por ID de métrica.
     */
    List<T15TopAeropuertoWps> findByMetricaDiariaIdOrderByRankingAsc(Integer metricaId);

    /**
     * Elimina todos los tops de una métrica.
     */
    void deleteByMetricaDiaria(T13MetricaDiaria metricaDiaria);
}
