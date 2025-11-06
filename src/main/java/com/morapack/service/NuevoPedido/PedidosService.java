package com.morapack.service.NuevoPedido;

import com.morapack.dto.NuevoPedido.FiltroPedidoDTO; // Importe necesario para el nuevo método
import com.morapack.dto.NuevoPedido.PedidoInternoDTO;
import com.morapack.dto.RespuestaDTO;
import com.morapack.model.NuevoPedido.T03Pedido;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T05Cliente;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.NuevoPedido.T03PedidoRepository;
import com.morapack.repository.T05ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier; // ¡Importe necesario!
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importe necesario
import java.util.Collections; // Importe necesario
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidosService {

    // Repositorios requeridos
    
    // ANOTACIÓN @Qualifier: Indica explícitamente a Spring qué bean inyectar.
    // El nombre "nuevoPedidoRepository" coincide con el que definimos en la interfaz T03PedidoRepository 
    // dentro del paquete NuevoPedido.
    @Qualifier("nuevoPedidoRepository")
    private final T03PedidoRepository pedidoRepository;

    private final T05ClienteRepository clienteRepository;
    private final T01AeropuertoRepository aeropuertoRepository;


    // ========================================================================
    // A. Mapeo de DTOs
    // ========================================================================

    /** Mapea la Entity T03Pedido a PedidoInternoDTO. */
    private PedidoInternoDTO pedidoToInternoDto(T03Pedido pedido) {
        
        return new PedidoInternoDTO(
            pedido.getId(),
            pedido.getT03IdCadena(),
            pedido.getT03FechaPedido(),
            pedido.getT03IdAeropDestino() != null ? pedido.getT03IdAeropDestino().getId() : null,
            pedido.getT03Cantidad(),
            pedido.getT03IdCliente()
        );
    }


    // ========================================================================
    // B. LÓGICA DE CONSULTA
    // ========================================================================

    /** Obtiene pedidos por nombre de cliente. */
    @Transactional(readOnly = true)
    public RespuestaDTO obtenerPedidosPorCliente(String nombreCliente) {
        Optional<T05Cliente> clienteOpt = clienteRepository.findByT05Nombre(nombreCliente);

        if (clienteOpt.isEmpty()) {
            return new RespuestaDTO("warning", "No se encontró el cliente con nombre: " + nombreCliente, null);
        }

        List<T03Pedido> pedidos = pedidoRepository.findByT03IdCliente(clienteOpt.get());
        List<PedidoInternoDTO> dtos = pedidos.stream().map(this::pedidoToInternoDto).toList();

        return new RespuestaDTO("success", String.format("Encontrados %d pedidos para el cliente.", dtos.size()), dtos);
    }

    /**
     * Obtiene pedidos por código ICAO de aeropuerto destino y fecha exacta.
     */
    @Transactional(readOnly = true)
    public RespuestaDTO obtenerPedidosPorVueloYFecha(String icaoCodigo, Instant fecha) {

        // 1. Buscar el aeropuerto por código ICAO (usando el método findBy, más eficiente que findAll)
        Optional<T01Aeropuerto> aeropuertoOpt = aeropuertoRepository.findByT01Codigoicao(icaoCodigo);

        if (aeropuertoOpt.isEmpty()) {
            return new RespuestaDTO("error", "Código ICAO de aeropuerto destino no válido o no encontrado: " + icaoCodigo, null);
        }

        T01Aeropuerto aeropuertoDestino = aeropuertoOpt.get();
        
        // 2. Buscar pedidos por el objeto T01Aeropuerto y la fecha exacta
        List<T03Pedido> pedidos = pedidoRepository.findByT03IdAeropDestinoAndT03FechaPedido(
            aeropuertoDestino,
            fecha
        );

        if (pedidos.isEmpty()) {
            return new RespuestaDTO("info", String.format("No se encontraron pedidos con destino %s en la fecha %s.", icaoCodigo, fecha), null);
        }

        // 3. Mapear a DTO de salida (Interno)
        List<PedidoInternoDTO> dtos = pedidos.stream().map(this::pedidoToInternoDto).toList();

        // 4. Retornar el resultado
        return new RespuestaDTO("success", String.format("Encontrados %d pedidos para destino %s.", dtos.size(), icaoCodigo), dtos);
    }


    /**
     * Filtra y obtiene pedidos por rango de fechas (obligatorio) y código ICAO (opcional).
     */
    @Transactional(readOnly = true)
    public RespuestaDTO filtrarPedidos(FiltroPedidoDTO filtro) {
        List<T03Pedido> pedidos;
        String icaoCodigo = filtro.getIcaoCodigo();
        Instant fechaInicio = filtro.getFechaInicio();
        Instant fechaFin = filtro.getFechaFin();

        // 1. Validación de fechas (control de rango)
        if (fechaInicio.isAfter(fechaFin)) {
            return new RespuestaDTO("error", "La fecha de inicio no puede ser posterior a la fecha de fin.", Collections.emptyList());
        }

        // 2. Determinar si se aplica el filtro de aeropuerto
        if (icaoCodigo != null && !icaoCodigo.trim().isEmpty()) {
            // Caso 1: Buscar por Destino ICAO y Rango de Fechas
            Optional<T01Aeropuerto> aeropuertoOpt = aeropuertoRepository.findByT01Codigoicao(icaoCodigo);

            if (aeropuertoOpt.isEmpty()) {
                return new RespuestaDTO("warning", "Código ICAO de aeropuerto destino no válido o no encontrado: " + icaoCodigo, Collections.emptyList());
            }

            T01Aeropuerto aeropuertoDestino = aeropuertoOpt.get();
            pedidos = pedidoRepository.findByT03IdAeropDestinoAndT03FechaPedidoBetween(
                    aeropuertoDestino,
                    fechaInicio,
                    fechaFin
            );
        } else {
            // Caso 2: Buscar solo por Rango de Fechas
            pedidos = pedidoRepository.findByT03FechaPedidoBetween(fechaInicio, fechaFin);
        }

        // 3. Mapear resultados y generar respuesta
        if (pedidos.isEmpty()) {
            String mensaje = (icaoCodigo != null && !icaoCodigo.trim().isEmpty())
                    ? String.format("No se encontraron pedidos para el destino %s entre %s y %s.", icaoCodigo, fechaInicio, fechaFin)
                    : String.format("No se encontraron pedidos en el rango de fechas entre %s y %s.", fechaInicio, fechaFin);
            
            return new RespuestaDTO("info", mensaje, Collections.emptyList());
        }

        List<PedidoInternoDTO> dtos = pedidos.stream().map(this::pedidoToInternoDto).toList();
        String mensajeExito = (icaoCodigo != null && !icaoCodigo.trim().isEmpty())
                ? String.format("Encontrados %d pedidos para destino %s en el rango de fechas.", dtos.size(), icaoCodigo)
                : String.format("Encontrados %d pedidos en el rango de fechas especificado.", dtos.size());

        return new RespuestaDTO("success", mensajeExito, dtos);
    }
}