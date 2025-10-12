// Archivo: com.morapack.repository.T11AvionRepository.java
package com.morapack.repository;

import com.morapack.model.T11Avion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface T11AvionRepository extends JpaRepository<T11Avion, Integer> {
    // Para buscar si el avión genérico ya existe y evitar duplicados de aviones (opcional)
    Optional<T11Avion> findByT11Matricula(String matricula);
}