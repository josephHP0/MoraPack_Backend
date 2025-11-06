package com.morapack.dto.NuevoPedido;

import java.time.Instant;

import com.morapack.model.T01Aeropuerto;

/**
 * DTO utilizado para enviar datos de pedidos a un endpoint o servicio externo.
 * Utiliza códigos ICAO (String) para los aeropuertos y añade el campo de Origen
 * requerido por el endpoint.
 *
 * @param idCadena ID de la cadena
 * @param fechaDespacho Fecha y hora prevista de despacho (puede ser la misma que T03_FECHA_PEDIDO)
 * @param aeropuertoOrigenIcao Código ICAO (ej: 'EBCI') del aeropuerto de origen (NUEVO CAMPO)
 * @param aeropuertoDestinoIcao Código ICAO (ej: 'SVMI') del aeropuerto destino
 * @param cantidad Cantidad de bultos
 * @param idCliente ID del cliente
 */
public record PedidoSalidaDTO(
    String idCadena,
    Instant fechaDespacho,
    T01Aeropuerto aeropuertoOrigen,
    T01Aeropuerto aeropuertoDestino,
    Integer cantidad,
    Integer idCliente
) {}