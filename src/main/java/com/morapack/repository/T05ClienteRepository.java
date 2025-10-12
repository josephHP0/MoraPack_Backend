package com.morapack.repository;

import com.morapack.model.T05Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface T05ClienteRepository extends JpaRepository<T05Cliente, Integer> {
    // Método para buscar al cliente por el nombre, que es el dato del archivo (ej. "0000006")
    Optional<T05Cliente> findByT05Nombre(String nombre);
}