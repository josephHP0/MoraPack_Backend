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

    @Query(value = "SELECT p.* FROM t02_pedido p " +
           "JOIN t01_aeropuerto a ON p.T02_ID_AEROP_DESTINO = a.T01_ID_AEROPUERTO " +
           "JOIN t05_ciudad c ON a.T01_ID_CIUDAD = c.T05_ID_CIUDAD " +
           "WHERE c.T05_ES_HUB = 0 AND p.T02_FECHA_PEDIDO BETWEEN :fechaInicio AND :fechaFin",
           nativeQuery = true)
    List<T02Pedido> findPedidosNoHubBetween(@Param("fechaInicio") Instant fechaInicio,
                                             @Param("fechaFin") Instant fechaFin);
}
