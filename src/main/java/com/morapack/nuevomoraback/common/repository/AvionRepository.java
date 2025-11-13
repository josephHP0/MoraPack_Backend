package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T06Avion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvionRepository extends JpaRepository<T06Avion, Integer> {
}
