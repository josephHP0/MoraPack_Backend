// Archivo: com.morapack.repository.T06VueloProgramadoRepository.java
package com.morapack.repository;

import com.morapack.model.T06VueloProgramado;
import com.morapack.model.T01Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface T06VueloProgramadoRepository extends JpaRepository<T06VueloProgramado, Integer> {

    // Asumiendo que dos vuelos son "duplicados" si tienen el mismo origen, destino,
    // y fechas de salida/llegada exactas (aunque usaremos la capacidad para diferenciar la hora si no usamos fechas completas)

    // Opción 1: Si usas Instant (necesitarías una fecha de contexto)
    List<T06VueloProgramado> findByT01IdaeropuertoorigenAndT01IdaeropuertodestinoAndT06Capacidadtotal(
            T01Aeropuerto origen, T01Aeropuerto destino, Integer capacidad);

    @Query("SELECT v FROM T06VueloProgramado v WHERE v.t01Idaeropuertoorigen = :aeropuerto OR v.t01Idaeropuertodestino = :aeropuerto")
    List<T06VueloProgramado> buscarPorOrigenODestino(@Param("aeropuerto") T01Aeropuerto aeropuerto);
}