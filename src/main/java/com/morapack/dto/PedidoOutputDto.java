package com.morapack.dto;

import lombok.Value;
import java.time.Instant;

/**
 * DTO de salida para un Pedido (T03Pedido) programado.
 */
@Value // Genera constructor con todos los campos, getters, equals, hashCode y toString.
public class PedidoOutputDto {

    // Identificador único del pedido (autogenerado)
    Integer id;
    // Fecha y hora en UTC en que se solicitó el pedido
    Instant fechaSolicitud;
    // Cantidad de paquetes/tamaño del pedido
    Integer cantidadPaquetes;
    // Estado global del pedido (ej. PENDIENTE, EN PROCESO, CUMPLIDO)
   String estadoGlobal;
    // ICAO del aeropuerto de origen (siempre el HUB fijo en la carga masiva)
    String icaoOrigen;
    // ICAO del aeropuerto de destino
    String icaoDestino;
    // ID numérico del cliente asociado (ID autogenerado en T05Cliente)
    String nombreCliente;

}