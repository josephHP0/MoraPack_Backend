package com.morapack.repository;

import com.morapack.model.T06VueloProgramado;
import com.morapack.model.T12Cancelacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio para gestión de cancelaciones de vuelos.
 */
@Repository
public interface T12CancelacionRepository extends JpaRepository<T12Cancelacion, Integer> {

    /**
     * Encuentra cancelaciones por vuelo.
     */
    List<T12Cancelacion> findByVueloProgramado(T06VueloProgramado vuelo);

    /**
     * Encuentra cancelaciones en un rango de fechas.
     */
    List<T12Cancelacion> findByFechaCancelacionBetween(Instant inicio, Instant fin);

    /**
     * Encuentra cancelaciones por origen.
     */
    List<T12Cancelacion> findByOrigen(String origen);

    /**
     * Encuentra todas las cancelaciones ordenadas por fecha.
     */
    List<T12Cancelacion> findAllByOrderByFechaCancelacionDesc();
}
