package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T01Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AeropuertoRepository extends JpaRepository<T01Aeropuerto, Integer> {
    Optional<T01Aeropuerto> findByT01CodigoIcao(String codigoIcao);

    @Query(value = "SELECT a.* FROM t01_aeropuerto a JOIN t05_ciudad c ON a.T01_ID_CIUDAD = c.T05_ID_CIUDAD WHERE c.T05_ES_HUB = 1", nativeQuery = true)
    List<T01Aeropuerto> findAeropuertosHub();

    @Query(value = "SELECT a.* FROM t01_aeropuerto a JOIN t05_ciudad c ON a.T01_ID_CIUDAD = c.T05_ID_CIUDAD WHERE c.T05_ES_HUB = 0", nativeQuery = true)
    List<T01Aeropuerto> findAeropuertosNoHub();
}
