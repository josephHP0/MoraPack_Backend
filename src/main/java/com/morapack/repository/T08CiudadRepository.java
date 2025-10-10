package com.morapack.repository;

import com.morapack.model.T08Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad T08Ciudad.
 * Proporciona métodos CRUD y consultas personalizadas para la gestión de ciudades.
 */
@Repository
public interface T08CiudadRepository extends JpaRepository<T08Ciudad, String> {

    /**
     * Busca una ciudad por su nombre exacto.
     * Spring Data JPA implementa este método automáticamente basado en el nombre.
     *
     * @param nombre El nombre de la ciudad a buscar.
     * @return un Optional que contiene la entidad T08Ciudad si se encuentra, o un Optional vacío si no.
     */
    Optional<T08Ciudad> findByT08Nombre(String nombre);

}