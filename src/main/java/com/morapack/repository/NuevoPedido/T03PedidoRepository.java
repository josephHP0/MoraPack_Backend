package com.morapack.repository.NuevoPedido;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.NuevoPedido.T03Pedido;
import com.morapack.model.T05Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; 

import java.time.Instant;
import java.util.List;

@Repository("nuevoPedidoRepository")
public interface T03PedidoRepository extends JpaRepository<T03Pedido, Integer> {

    // 1. Obtener por Cliente
    List<T03Pedido> findByT03IdCliente(T05Cliente cliente);

    // 2. Obtener por Aeropuerto DESTINO y fecha EXACTA
    List<T03Pedido> findByT03IdAeropDestinoAndT03FechaPedido(T01Aeropuerto destino, Instant fecha);

    
    List<T03Pedido> findByT03FechaPedidoBetween(Instant fechaInicio, Instant fechaFin);

    /**
     * Obtiene pedidos por Aeropuerto Destino (obligatorio) y Rango de Fecha (obligatorio).
     */
    List<T03Pedido> findByT03IdAeropDestinoAndT03FechaPedidoBetween(
            T01Aeropuerto destino,
            Instant fechaInicio,
            Instant fechaFin);

   
    // 4. Obtener por Aeropuerto Destino, Cantidad y Rango de Fecha
    List<T03Pedido> findByT03IdAeropDestinoAndT03CantidadAndT03FechaPedidoBetween(
            T01Aeropuerto destino,
            Integer cantidad,
            Instant fechaInicio,
            Instant fechaFin);
}


