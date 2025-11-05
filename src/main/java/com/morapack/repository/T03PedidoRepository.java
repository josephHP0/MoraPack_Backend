package com.morapack.repository;

import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import com.morapack.model.T05Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface T03PedidoRepository extends JpaRepository<T03Pedido, Integer> {
    // 1. Obtener por Cliente (relación ManyToOne)
    List<T03Pedido> findByT03Idcliente(T05Cliente cliente);

    // 2. Obtener por Aeropuerto y Rango de Fecha (para el vuelo/fecha de solicitud)
    List<T03Pedido> findByT01IdaeropuertoorigenOrT01IdaeropuertodestinoAndT03FechacreacionBetween(
            T01Aeropuerto origen, T01Aeropuerto destino, Instant fechaInicio, Instant fechaFin);

    List<T03Pedido> findByT01IdaeropuertodestinoAndT03CantidadpaquetesAndT03FechacreacionBetween(
            T01Aeropuerto destino,
            Integer cantidadPaquetes,
            Instant fechaInicio,
            Instant fechaFin);
            
    List<T03Pedido> findByT03FechacreacionBetween(Instant fechaInicio, Instant fechaFin);
}