package com.morapack.nuevomoraback.planificacion.repository;

import com.morapack.nuevomoraback.planificacion.domain.T09TramoAsignado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface T09TramoAsignadoRepository extends JpaRepository<T09TramoAsignado, Integer> {

    @Query("SELECT t FROM T09TramoAsignado t WHERE t.vueloProgramado.id = :vueloId")
    List<T09TramoAsignado> findByVueloId(@Param("vueloId") Integer vueloId);
}
