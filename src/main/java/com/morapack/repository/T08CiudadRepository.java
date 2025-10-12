package com.morapack.repository;

import com.morapack.model.T08Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface T08CiudadRepository extends JpaRepository<T08Ciudad, Integer> {
    // Búsqueda por nombre (unique)
    Optional<T08Ciudad> findByT08Nombre(String nombre);

    // Verificar existencia por nombre (para duplicados)
    boolean existsByT08Nombre(String nombre);

    // Opcional: Por continente
    List<T08Ciudad> findByT08Continente(String continente);
}
