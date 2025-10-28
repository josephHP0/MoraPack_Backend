package com.morapack.service;

import com.morapack.dto.PedidoInputDto;
import com.morapack.dto.PedidoOutputDto;
import com.morapack.dto.RespuestaDTO;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import com.morapack.model.T05Cliente;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.T03PedidoRepository;
import com.morapack.repository.T05ClienteRepository;
import com.morapack.util.UtilArchivos;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class PedidoService {
    @Autowired private T03PedidoRepository pedidoRepository; // T03 ahora
    @Autowired private T05ClienteRepository clienteRepository; // T05 ahora
    @Autowired private T01AeropuertoRepository aeropuertoRepository;

    private static final String ICAO_HUB_ORIGEN = "SKBO"; // Mantenemos el HUB de origen fijo
    private static final LocalDateTime FECHA_BASE = LocalDateTime.of(2025, 1, 1, 0, 0);

    // ========================================================================
    // LÓGICA DE PROCESAMIENTO
    // ========================================================================

    @Transactional
    public RespuestaDTO procesarPedido(PedidoInputDto inputDto) {

        // 1. Obtener Aeropuerto HUB de origen
        Optional<T01Aeropuerto> origenOpt = aeropuertoRepository.findByT01Codigoicao(ICAO_HUB_ORIGEN);
        if (origenOpt.isEmpty()) {
            return new RespuestaDTO("error", "Aeropuerto HUB de origen (" + ICAO_HUB_ORIGEN + ") no encontrado. Debe cargar aeropuertos primero.", null);
        }
        T01Aeropuerto aeropuertoOrigen = origenOpt.get();

        // 2. Obtener Aeropuerto de destino
        Optional<T01Aeropuerto> destinoOpt = aeropuertoRepository.findByT01Codigoicao(inputDto.getIcaoDestino());
        if (destinoOpt.isEmpty()) {
            return new RespuestaDTO("error", "Aeropuerto ICAO de destino no encontrado: " + inputDto.getIcaoDestino(), null);
        }
        T01Aeropuerto destino = destinoOpt.get();

        // 2. Obtener la Fecha de Solicitud (ya corregida para usar dia/hora/minuto)
        Instant fechaSolicitud;
        if (inputDto.getDia() != null && inputDto.getHora() != null && inputDto.getMinuto() != null) {
            fechaSolicitud = crearInstantPedido(inputDto.getDia(), inputDto.getHora(), inputDto.getMinuto());
        } else {
            return new RespuestaDTO("error", "Faltan datos de día, hora o minuto para establecer la fecha de solicitud.", null);
        }

        // ********* INICIO DE VALIDACIÓN DE UNICIDAD *********

        // Definir el rango de 1 minuto para la búsqueda (ej. de 10:00:00 a 10:00:59)
        // Usamos +/- 1 segundo para capturar exactamente el mismo minuto.
        Instant fechaInicio = fechaSolicitud.minus(1, ChronoUnit.SECONDS);
        Instant fechaFin = fechaSolicitud.plus(1, ChronoUnit.SECONDS);

        List<T03Pedido> pedidosExistentes = pedidoRepository
                .findByT01IdaeropuertodestinoAndT03CantidadpaquetesAndT03FechacreacionBetween(
                        destino,
                        inputDto.getTamanho(),
                        fechaInicio,
                        fechaFin);

        if (!pedidosExistentes.isEmpty()) {
            return new RespuestaDTO("warning",
                    String.format("Pedido duplicado detectado: %s, Destino: %s, Cantidad: %d, Fecha: %s.",
                            inputDto.getNombreCliente(), inputDto.getIcaoDestino(), inputDto.getTamanho(), fechaSolicitud.toString()),
                    null);
        }

        // ********* FIN DE VALIDACIÓN DE UNICIDAD *********

        // 3. Obtener o Crear Cliente
        T05Cliente cliente = crearOObtenerCliente(inputDto.getNombreCliente());

        // 4. Crear el Pedido (T03Pedido)
        T03Pedido pedido = new T03Pedido();
        pedido.setT01Idaeropuertoorigen(aeropuertoOrigen);
        pedido.setT01Idaeropuertodestino(destino);
        pedido.setT03Idcliente(cliente);
        pedido.setT03Cantidadpaquetes(inputDto.getTamanho());
        pedido.setT03Fechacreacion(fechaSolicitud);
        pedido.setT03Estadoglobal("PENDIENTE"); // Estado inicial del pedido

        // 5. Guardar
        T03Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 6. Respuesta
        PedidoOutputDto outputDto = pedidoToOutputDto(pedidoGuardado);
        return new RespuestaDTO("success", "Pedido creado/guardado exitosamente.", outputDto);
    }


    @Transactional
    public RespuestaDTO cargarPedidosArchivo(MultipartFile archivo) {
        try {
            // Asume que UtilArchivos.cargarPedidos ya existe y funciona.
            List<PedidoInputDto> inputDtos = UtilArchivos.cargarPedidos(archivo);

            List<String> errores = new ArrayList<>();
            int totalGuardados = 0;

            for (PedidoInputDto inputDto : inputDtos) {
                try {
                    // Llama al método central que ya contiene la unicidad y la creación.
                    RespuestaDTO resultado = procesarPedido(inputDto);

                    if ("error".equals(resultado.getStatus())) {
                        errores.add(String.format("Cliente %s, Destino %s: %s", inputDto.getNombreCliente(), inputDto.getIcaoDestino(), resultado.getMensaje()));
                    } else if ("warning".equals(resultado.getStatus())) {
                        // Si es 'warning' significa que es un duplicado ignorado.
                        errores.add(String.format("Cliente %s, Destino %s: ADVERTENCIA - %s", inputDto.getNombreCliente(), inputDto.getIcaoDestino(), resultado.getMensaje()));
                    } else if ("success".equals(resultado.getStatus())) {
                        totalGuardados++;
                    }
                } catch (Exception e) {
                    errores.add(String.format("Cliente %s, Destino %s: Error interno al procesar - %s", inputDto.getNombreCliente(), inputDto.getIcaoDestino(), e.getMessage()));
                }
            }

            Map<String, Object> data = Map.of(
                    "totalProcesados", inputDtos.size(),
                    "totalGuardados", totalGuardados,
                    "errores", errores
            );

            return new RespuestaDTO("success", String.format("Carga masiva completada. %d guardados, %d errores/advertencias.", totalGuardados, errores.size()), data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error procesando archivo de pedidos: " + e.getMessage(), null);
        }
    }

    // ========================================================================
    // B. LÓGICA DE CONSULTA
    // ========================================================================

    /** Obtiene pedidos por nombre de cliente. */
    public RespuestaDTO obtenerPedidosPorCliente(String nombreCliente) {
        Optional<T05Cliente> clienteOpt = clienteRepository.findByT05Nombre(nombreCliente);

        if (clienteOpt.isEmpty()) {
            return new RespuestaDTO("warning", "No se encontró el cliente con nombre: " + nombreCliente, null);
        }

        List<T03Pedido> pedidos = pedidoRepository.findByT03Idcliente(clienteOpt.get());
        List<PedidoOutputDto> dtos = pedidos.stream().map(this::pedidoToOutputDto).toList();

        return new RespuestaDTO("success", String.format("Encontrados %d pedidos para el cliente.", dtos.size()), dtos);
    }

    /** Obtiene pedidos por aeropuerto y fecha de solicitud. */
    public RespuestaDTO obtenerPedidosPorVueloYFecha(String icaoCodigo, Instant fecha) {

        Optional<T01Aeropuerto> aeropuertoOpt = aeropuertoRepository.findByT01Codigoicao(icaoCodigo);
        if (aeropuertoOpt.isEmpty()) {
            return new RespuestaDTO("error", "Aeropuerto ICAO no encontrado: " + icaoCodigo, null);
        }
        T01Aeropuerto aeropuerto = aeropuertoOpt.get();

        // Define el rango de 24 horas para la búsqueda
        Instant fechaInicio = fecha.truncatedTo(ChronoUnit.DAYS);
        Instant fechaFin = fechaInicio.plus(1, ChronoUnit.DAYS);

        // Busca pedidos donde el aeropuerto sea origen O destino y caiga en el día.
        List<T03Pedido> pedidos = pedidoRepository
                .findByT01IdaeropuertoorigenOrT01IdaeropuertodestinoAndT03FechacreacionBetween(
                        aeropuerto, aeropuerto, fechaInicio, fechaFin); // Se pasa 'aeropuerto' dos veces

        List<PedidoOutputDto> dtos = pedidos.stream().map(this::pedidoToOutputDto).toList();

        return new RespuestaDTO("success", String.format("Encontrados %d pedidos para el vuelo/fecha.", dtos.size()), dtos);
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    private T05Cliente crearOObtenerCliente(String nombreCliente) {
        Optional<T05Cliente> clienteOpt = clienteRepository.findByT05Nombre(nombreCliente);

        if (clienteOpt.isPresent()) {
            return clienteOpt.get(); // Cliente existente
        }

        // Crear nuevo cliente si no existe
        T05Cliente nuevoCliente = new T05Cliente();
        nuevoCliente.setT05Nombre(nombreCliente); // El nombre es el dato del archivo/input
        nuevoCliente.setT05Contacto("Contacto genérico (Archivo)");

        return clienteRepository.save(nuevoCliente);
    }

    private Instant crearInstantPedido(Integer dia, Integer hora, Integer minuto) {
        return LocalDateTime.of(FECHA_BASE.getYear(), FECHA_BASE.getMonth(), dia, hora, minuto)
                .toInstant(ZoneOffset.UTC);
    }

    // Deberías crear el PedidoOutputDto
    private PedidoOutputDto pedidoToOutputDto(T03Pedido entidad) {
        return new PedidoOutputDto(
                entidad.getId(),
                entidad.getT03Fechacreacion(),
                entidad.getT03Cantidadpaquetes(),
                entidad.getT03Estadoglobal(),
                entidad.getT01Idaeropuertoorigen().getT01Codigoicao(),
                entidad.getT01Idaeropuertodestino().getT01Codigoicao(),
                entidad.getT03Idcliente().getT05Nombre()
        );
    }
}
