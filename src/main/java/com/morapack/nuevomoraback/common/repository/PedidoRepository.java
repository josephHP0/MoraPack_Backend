package com.morapack.nuevomoraback.common.repository;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<T02Pedido, Integer> {

    @Query("SELECT p FROM T02Pedido p WHERE p.t02FechaPedido BETWEEN :fechaInicio AND :fechaFin")
    List<T02Pedido> findByFechaPedidoBetween(@Param("fechaInicio") Instant fechaInicio,
                                              @Param("fechaFin") Instant fechaFin);

    @Query("SELECT p FROM T02Pedido p WHERE p.t02FechaPedido >= :fechaInicio")
    List<T02Pedido> findByFechaPedidoAfter(@Param("fechaInicio") Instant fechaInicio);

    @Query("SELECT p FROM T02Pedido p JOIN T01Aeropuerto a ON p.t02IdAeropDestino.id = a.id " +
           "JOIN T05Ciudad c ON a.t01IdCiudad = c.id WHERE c.t05EsHub = false " +
           "AND p.t02FechaPedido BETWEEN :fechaInicio AND :fechaFin")
    List<T02Pedido> findPedidosNoHubBetween(@Param("fechaInicio") Instant fechaInicio,
                                             @Param("fechaFin") Instant fechaFin);
}
