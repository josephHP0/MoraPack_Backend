package com.morapack.repository;

import com.morapack.model.T03Pedido;
import com.morapack.model.T06VueloProgramado;
import com.morapack.model.T09Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestión de asignaciones de pedidos a vuelos.
 */
@Repository
public interface T09AsignacionRepository extends JpaRepository<T09Asignacion, Integer> {

    /**
     * Encuentra todas las asignaciones de un pedido específico.
     */
    List<T09Asignacion> findByT03Idpedido(T03Pedido pedido);

    /**
     * Encuentra todas las asignaciones de un pedido específico ordenadas por orden.
     */
    List<T09Asignacion> findByT03IdpedidoOrderByT09OrdenAsc(T03Pedido pedido);

    /**
     * Encuentra todas las asignaciones de un vuelo específico.
     */
    List<T09Asignacion> findByT06Idtramovuelo(T06VueloProgramado vuelo);

    /**
     * Encuentra asignaciones por ID de pedido.
     */
    @Query("SELECT a FROM T09Asignacion a WHERE a.t03Idpedido.id = :pedidoId ORDER BY a.t09Orden ASC")
    List<T09Asignacion> findByPedidoId(@Param("pedidoId") Integer pedidoId);

    /**
     * Encuentra asignaciones por ID de vuelo.
     */
    @Query("SELECT a FROM T09Asignacion a WHERE a.t06Idtramovuelo.id = :vueloId")
    List<T09Asignacion> findByVueloId(@Param("vueloId") Integer vueloId);

    /**
     * Elimina todas las asignaciones de un pedido.
     */
    void deleteByT03Idpedido(T03Pedido pedido);
}
