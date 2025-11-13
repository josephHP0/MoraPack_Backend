package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T05Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiudadRepository extends JpaRepository<T05Ciudad, Integer> {
    List<T05Ciudad> findByT05EsHub(Boolean esHub);
}
