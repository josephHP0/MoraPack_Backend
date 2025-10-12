package com.morapack.repository;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T08Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface T01AeropuertoRepository extends JpaRepository<T01Aeropuerto, Integer> {
    // Búsqueda por ICAO (unique)
    Optional<T01Aeropuerto> findByT01Codigoicao(String icao);

    // Verificar existencia por ICAO (para duplicados)
    boolean existsByT01Codigoicao(String icao);

    // Opcional: Por ciudad (FK)
    List<T01Aeropuerto> findByT08Idciudad(T08Ciudad ciudad);

    // Por capacidad o GMT (ej. para queries futuras)
    List<T01Aeropuerto> findByT01CapacidadGreaterThanEqual(Integer minCapacidad);
}
