package com.morapack.dto.NuevoPedido;

import java.time.Instant;

/**
 * DTO utilizado para transferir datos internamente desde la base de datos
 * antes de cualquier transformación para APIs externas.
 * Mantiene el ID numérico del aeropuerto destino (T03_ID_AEROP_DESTINO).
 *
 * @param id ID del pedido (T03_ID)
 * @param idCadena ID de la cadena (T03_ID_CADENA)
 * @param fechaPedido Fecha y hora del pedido (T03_FECHA_PEDIDO)
 * @param idAeropDestino ID numérico del aeropuerto destino (T01_idAeropuerto)
 * @param cantidad Cantidad de bultos (T03_CANTIDAD)
 * @param idCliente ID del cliente (T03_ID_CLIENTE)
 */
public record PedidoInternoDTO(
    Integer id,
    String idCadena,
    Instant fechaPedido,
    Integer idAeropDestino,
    Integer cantidad,
    Integer idCliente
) {}