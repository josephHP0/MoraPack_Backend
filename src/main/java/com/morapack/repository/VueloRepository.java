package com.morapack.repository;
import com.morapack.model.T06VueloProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface VueloRepository extends JpaRepository<T06VueloProgramado, Integer> {
    // Métodos custom: findByOrigenIataAndHoraSalidaUtc, etc.
}
