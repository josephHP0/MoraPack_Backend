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

    @Query("SELECT a FROM T01Aeropuerto a JOIN T05Ciudad c ON a.t01IdCiudad = c.id WHERE c.t05EsHub = true")
    List<T01Aeropuerto> findAeropuertosHub();

    @Query("SELECT a FROM T01Aeropuerto a JOIN T05Ciudad c ON a.t01IdCiudad = c.id WHERE c.t05EsHub = false")
    List<T01Aeropuerto> findAeropuertosNoHub();
}
