package com.morapack.nuevomoraback.planificacion.repository;

import com.morapack.nuevomoraback.planificacion.domain.T11MetricasSimulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface T11MetricasSimulacionRepository extends JpaRepository<T11MetricasSimulacion, Integer> {
}
