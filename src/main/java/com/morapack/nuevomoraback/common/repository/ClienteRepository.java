package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T03Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<T03Cliente, Integer> {
}
