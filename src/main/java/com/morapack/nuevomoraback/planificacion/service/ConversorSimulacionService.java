package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.repository.VueloProgramadoRepository;
import com.morapack.nuevomoraback.planificacion.domain.*;
import com.morapack.nuevomoraback.planificacion.dto.*;
import com.morapack.nuevomoraback.planificacion.repository.T08RutaPlaneadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para convertir entidades de simulación a DTOs detallados para el frontend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversorSimulacionService {

    private final VueloProgramadoRepository vueloRepository;
    private final T08RutaPlaneadaRepository rutaPlaneadaRepository;
    private final CalculadorSLA calculadorSLA;

    /**
     * Convierte un resultado de simulación a DTO detallado
     */
    public SimulacionDetalladaDTO convertirADetallado(
            T10ResultadoSimulacion resultado,
            List<T08RutaPlaneada> rutas,
            Integer duracionBloqueHoras,
            Integer numeroBloquesEjecutados) {

        log.info("Convirtiendo simulación a DTO detallado: {} rutas planificadas", rutas.size());

        SimulacionDetalladaDTO dto = new SimulacionDetalladaDTO();

        // Metadata
        dto.setIdResultado(resultado.getId());
        dto.setTipoSimulacion(resultado.getTipoSimulacion().name());
        dto.setFechaInicio(resultado.getFechaInicio());
        dto.setFechaFin(resultado.getFechaFin());
        dto.setFechaEjecucion(resultado.getFechaEjecucion());
        dto.setDuracionMs(resultado.getDuracionMs());
        dto.setEstado(resultado.getEstado().name());
        dto.setMensaje(resultado.getMensaje());
        dto.setDuracionBloqueHoras(duracionBloqueHoras);
        dto.setNumeroBloquesEjecutados(numeroBloquesEjecutados);

        // Métricas
        if (resultado.getMetricas() != null) {
            T11MetricasSimulacion m = resultado.getMetricas();
            dto.setMetricas(new MetricasDTO(
                m.getTotalPedidos(),
                m.getPedidosEntregados(),
                m.getPedidosEnTransito(),
                m.getPedidosRechazados(),
                m.getCumplimientoSla(),
                m.getOcupacionPromedio(),
                m.getVuelosUtilizados(),
                m.getVuelosCancelados()
            ));
        }

        // Convertir pedidos con sus rutas
        List<PedidoRutaDTO> pedidosDTO = rutas.stream()
            .map(this::convertirPedidoConRuta)
            .collect(Collectors.toList());
        dto.setPedidos(pedidosDTO);

        // Convertir vuelos con ocupación
        List<VueloDetalladoDTO> vuelosDTO = construirVuelosDetallados(rutas, resultado.getFechaInicio(), resultado.getFechaFin());
        dto.setVuelos(vuelosDTO);

        // Estadísticas detalladas
        dto.setEstadisticas(calcularEstadisticasDetalladas(rutas, vuelosDTO));

        log.info("Conversión completada: {} vuelos, {} pedidos", vuelosDTO.size(), pedidosDTO.size());

        return dto;
    }

    /**
     * Convierte una ruta planeada a PedidoRutaDTO
     */
    private PedidoRutaDTO convertirPedidoConRuta(T08RutaPlaneada ruta) {
        PedidoRutaDTO dto = new PedidoRutaDTO();

        // Información del pedido
        dto.setPedidoId(ruta.getPedido().getId());
        dto.setIdCadena(ruta.getPedido().getT02IdCadena());
        dto.setFechaPedido(ruta.getPedido().getT02FechaPedido());
        dto.setCantidad(ruta.getPedido().getT02Cantidad());
        dto.setClienteNombre("Generico");

        // Destino
        dto.setDestinoCodigoICAO(ruta.getPedido().getT02IdAeropDestino().getT01CodigoIcao());
        dto.setDestinoCiudad(ruta.getPedido().getT02IdAeropDestino().getT01Alias());

        // Estado
        dto.setEstado(ruta.getEstado().name());
        dto.setCumpleSla(ruta.getCumpleSla());
        dto.setFechaEntregaEstimada(ruta.getFechaEntregaEstimada());

        // Calcular fecha límite SLA
        Instant fechaLimite = calculadorSLA.calcularFechaLimite(ruta.getPedido());
        dto.setFechaLimiteSla(fechaLimite);

        // Convertir tramos
        List<PedidoRutaDTO.TramoRutaDTO> tramosDTO = ruta.getTramosAsignados().stream()
            .sorted(Comparator.comparing(T09TramoAsignado::getOrdenEnRuta))
            .map(this::convertirTramo)
            .collect(Collectors.toList());
        dto.setTramos(tramosDTO);

        return dto;
    }

    /**
     * Convierte un tramo a TramoRutaDTO
     */
    private PedidoRutaDTO.TramoRutaDTO convertirTramo(T09TramoAsignado tramo) {
        T04VueloProgramado vuelo = tramo.getVueloProgramado();

        return new PedidoRutaDTO.TramoRutaDTO(
            tramo.getOrdenEnRuta().intValue(),
            vuelo.getId(),
            vuelo.getT01IdAeropuertoOrigen().getT01CodigoIcao(),
            vuelo.getT01IdAeropuertoOrigen().getT01Alias(),
            vuelo.getT01IdAeropuertoDestino().getT01CodigoIcao(),
            vuelo.getT01IdAeropuertoDestino().getT01Alias(),
            vuelo.getT04FechaSalida(),
            vuelo.getT04FechaLlegada(),
            tramo.getEsVueloFinal(),
            tramo.getCantidadProductos()
        );
    }

    /**
     * Construye lista de vuelos detallados con su ocupación
     */
    private List<VueloDetalladoDTO> construirVuelosDetallados(List<T08RutaPlaneada> rutas, Instant fechaInicio, Instant fechaFin) {

        // Mapa para acumular información de cada vuelo utilizado
        Map<Integer, VueloDetalladoDTO> vuelosMap = new HashMap<>();

        // Procesar todos los tramos de todas las rutas
        for (T08RutaPlaneada ruta : rutas) {
            for (T09TramoAsignado tramo : ruta.getTramosAsignados()) {
                T04VueloProgramado vuelo = tramo.getVueloProgramado();
                Integer vueloId = vuelo.getId();

                // Si el vuelo no está en el mapa, crearlo
                if (!vuelosMap.containsKey(vueloId)) {
                    VueloDetalladoDTO vueloDTO = crearVueloDetallado(vuelo);
                    vuelosMap.put(vueloId, vueloDTO);
                }

                // Agregar pedido a la lista de asignados
                VueloDetalladoDTO vueloDTO = vuelosMap.get(vueloId);
                VueloDetalladoDTO.PedidoEnVueloDTO pedidoEnVuelo = new VueloDetalladoDTO.PedidoEnVueloDTO(
                    ruta.getPedido().getId(),
                    ruta.getPedido().getT02IdCadena(),
                    tramo.getCantidadProductos(),
                    ruta.getPedido().getT02IdAeropDestino().getT01Alias(),
                    tramo.getEsVueloFinal()
                );
                vueloDTO.getPedidosAsignados().add(pedidoEnVuelo);

                // Actualizar capacidad ocupada
                vueloDTO.setCapacidadOcupada(vueloDTO.getCapacidadOcupada() + tramo.getCantidadProductos());
            }
        }

        // Recalcular capacidad disponible y porcentajes
        vuelosMap.values().forEach(this::recalcularCapacidades);

        return new ArrayList<>(vuelosMap.values());
    }

    /**
     * Crea VueloDetalladoDTO con información básica
     */
    private VueloDetalladoDTO crearVueloDetallado(T04VueloProgramado vuelo) {
        VueloDetalladoDTO dto = new VueloDetalladoDTO();

        dto.setVueloId(vuelo.getId());
        dto.setCodigoOrigenICAO(vuelo.getT01IdAeropuertoOrigen().getT01CodigoIcao());
        dto.setNombreOrigenCiudad(vuelo.getT01IdAeropuertoOrigen().getT01Alias());
        dto.setCodigoDestinoICAO(vuelo.getT01IdAeropuertoDestino().getT01CodigoIcao());
        dto.setNombreDestinoCiudad(vuelo.getT01IdAeropuertoDestino().getT01Alias());
        dto.setFechaSalida(vuelo.getT04FechaSalida());
        dto.setFechaLlegada(vuelo.getT04FechaLlegada());

        if (vuelo.getT11IdAvion() != null) {
            dto.setMatriculaAvion(vuelo.getT11IdAvion().getT06Matricula());
            dto.setModeloAvion(vuelo.getT11IdAvion().getT06Modelo());
        }

        dto.setCapacidadTotal(vuelo.getT04CapacidadTotal());
        dto.setCapacidadOcupada(0); // Se actualizará al procesar pedidos
        dto.setEstadoCapacidad(vuelo.getT04EstadoCapacidad());
        dto.setPedidosAsignados(new ArrayList<>());

        return dto;
    }

    /**
     * Recalcula capacidades y porcentajes de un vuelo
     */
    private void recalcularCapacidades(VueloDetalladoDTO vuelo) {
        vuelo.setCapacidadDisponible(vuelo.getCapacidadTotal() - vuelo.getCapacidadOcupada());

        double porcentaje = (vuelo.getCapacidadOcupada() * 100.0) / vuelo.getCapacidadTotal();
        vuelo.setPorcentajeOcupacion(Math.round(porcentaje * 100.0) / 100.0);

        // Actualizar estado de capacidad
        if (vuelo.getCapacidadOcupada() > vuelo.getCapacidadTotal()) {
            vuelo.setEstadoCapacidad("SOBRECARGA");
        } else {
            vuelo.setEstadoCapacidad("NORMAL");
        }
    }

    /**
     * Calcula estadísticas detalladas
     */
    private SimulacionDetalladaDTO.EstadisticasDetalladasDTO calcularEstadisticasDetalladas(
            List<T08RutaPlaneada> rutas,
            List<VueloDetalladoDTO> vuelos) {

        SimulacionDetalladaDTO.EstadisticasDetalladasDTO stats =
            new SimulacionDetalladaDTO.EstadisticasDetalladasDTO();

        // Vuelos
        stats.setTotalVuelosDisponibles(vuelos.size()); // Solo contamos vuelos utilizados
        stats.setVuelosUtilizados(vuelos.size());
        stats.setVuelosNoUtilizados(0); // Difícil calcular sin saber total de vuelos expandidos
        stats.setPorcentajeUtilizacionVuelos(100.0); // Solo mostramos utilizados

        // Ocupación
        if (!vuelos.isEmpty()) {
            double ocupacionPromedio = vuelos.stream()
                .mapToDouble(VueloDetalladoDTO::getPorcentajeOcupacion)
                .average()
                .orElse(0.0);
            stats.setOcupacionPromedioVuelosUtilizados(Math.round(ocupacionPromedio * 100.0) / 100.0);
            stats.setOcupacionPromedioTodosVuelos(ocupacionPromedio);
        }

        long vuelosSobrecarga = vuelos.stream()
            .filter(v -> "SOBRECARGA".equals(v.getEstadoCapacidad()))
            .count();
        stats.setVuelosConSobrecarga((int) vuelosSobrecarga);

        // Pedidos por estado
        Map<T08RutaPlaneada.EstadoRuta, Long> pedidosPorEstado = rutas.stream()
            .collect(Collectors.groupingBy(T08RutaPlaneada::getEstado, Collectors.counting()));

        stats.setPedidosPendientes(pedidosPorEstado.getOrDefault(T08RutaPlaneada.EstadoRuta.PENDIENTE, 0L).intValue());
        stats.setPedidosEnTransito(pedidosPorEstado.getOrDefault(T08RutaPlaneada.EstadoRuta.EN_TRANSITO, 0L).intValue());
        stats.setPedidosEntregados(pedidosPorEstado.getOrDefault(T08RutaPlaneada.EstadoRuta.ENTREGADO, 0L).intValue());
        stats.setPedidosRechazados(pedidosPorEstado.getOrDefault(T08RutaPlaneada.EstadoRuta.RECHAZADO, 0L).intValue());

        // SLA
        long cumplenSla = rutas.stream().filter(T08RutaPlaneada::getCumpleSla).count();
        long noCumplenSla = rutas.size() - cumplenSla;

        stats.setPedidosCumplenSla((int) cumplenSla);
        stats.setPedidosNoCumplenSla((int) noCumplenSla);

        if (rutas.size() > 0) {
            double porcentajeSla = (cumplenSla * 100.0) / rutas.size();
            stats.setPorcentajeCumplimientoSla(Math.round(porcentajeSla * 100.0) / 100.0);
        }

        return stats;
    }

    /**
     * Convierte rutas de un bloque específico a BloqueSimulacionResponse
     * Diseñado para streaming incremental
     */
    public BloqueSimulacionResponse convertirABloqueResponse(
            Integer idResultado,
            Instant fechaInicio,
            Instant fechaFin,
            List<T08RutaPlaneada> rutasBloque,
            MetricasBloqueDTO metricas) {

        log.info("Convirtiendo bloque a response: {} rutas", rutasBloque.size());

        // Construir vuelos simulados para este bloque
        List<VueloSimuladoDTO> vuelosDTO = construirVuelosSimulados(rutasBloque, fechaInicio, fechaFin);

        // Construir pedidos detallados del bloque
        List<PedidoDetalleDTO> pedidosDTO = rutasBloque.stream()
            .map(this::convertirAPedidoDetalle)
            .collect(Collectors.toList());

        // Construir rutas completas del bloque
        List<RutaCompletaDTO> rutasDTO = rutasBloque.stream()
            .map(this::convertirARutaCompleta)
            .collect(Collectors.toList());

        return BloqueSimulacionResponse.builder()
            .idResultadoSimulacion(idResultado)
            .fechaInicio(fechaInicio)
            .fechaFin(fechaFin)
            .numeroBloque(1) // El front puede incrementar esto si necesita
            .vuelos(vuelosDTO)
            .pedidos(pedidosDTO)
            .rutas(rutasDTO)
            .metricas(metricas)
            .hayMasBloques(false) // Se setea en el service
            .build();
    }

    /**
     * Construye DTOs de vuelos simulados para un bloque
     */
    private List<VueloSimuladoDTO> construirVuelosSimulados(
            List<T08RutaPlaneada> rutasBloque,
            Instant fechaInicio,
            Instant fechaFin) {

        // Extraer todos los vuelos usados en el bloque
        Map<Integer, T04VueloProgramado> vuelosUsados = new HashMap<>();
        Map<Integer, Integer> ocupacionPorVuelo = new HashMap<>();

        for (T08RutaPlaneada ruta : rutasBloque) {
            for (T09TramoAsignado tramo : ruta.getTramosAsignados()) {
                T04VueloProgramado vuelo = tramo.getVueloProgramado();

                // Solo incluir vuelos que salen dentro del rango del bloque
                if (!vuelo.getT04FechaSalida().isBefore(fechaInicio) &&
                    vuelo.getT04FechaSalida().isBefore(fechaFin)) {

                    vuelosUsados.put(vuelo.getId(), vuelo);

                    int cantidadActual = ocupacionPorVuelo.getOrDefault(vuelo.getId(), 0);
                    ocupacionPorVuelo.put(vuelo.getId(), cantidadActual + tramo.getCantidadProductos());
                }
            }
        }

        // Convertir a DTOs
        return vuelosUsados.values().stream()
            .map(vuelo -> convertirAVueloSimulado(vuelo, ocupacionPorVuelo.get(vuelo.getId())))
            .sorted(Comparator.comparing(VueloSimuladoDTO::getFechaSalida))
            .collect(Collectors.toList());
    }

    /**
     * Convierte un vuelo a DTO simulado
     */
    private VueloSimuladoDTO convertirAVueloSimulado(T04VueloProgramado vuelo, Integer ocupacion) {
        return VueloSimuladoDTO.builder()
            .id(vuelo.getId())
            .codigoOrigenICAO(vuelo.getT01IdAeropuertoOrigen().getT01CodigoIcao())
            .nombreOrigenCiudad(vuelo.getT01IdAeropuertoOrigen().getT01Alias())
            .codigoDestinoICAO(vuelo.getT01IdAeropuertoDestino().getT01CodigoIcao())
            .nombreDestinoCiudad(vuelo.getT01IdAeropuertoDestino().getT01Alias())
            .fechaSalida(vuelo.getT04FechaSalida())
            .fechaLlegada(vuelo.getT04FechaLlegada())
            .capacidadTotal(vuelo.getT04CapacidadTotal())
            .capacidadOcupada(ocupacion != null ? ocupacion : 0)
            .capacidadDisponible(vuelo.getT04CapacidadTotal() - (ocupacion != null ? ocupacion : 0))
            .estado(vuelo.getT04Estado())
            .build();
    }

    /**
     * Convierte ruta a pedido detalle
     */
    private PedidoDetalleDTO convertirAPedidoDetalle(T08RutaPlaneada ruta) {
        return PedidoDetalleDTO.builder()
            .idPedido(ruta.getPedido().getId())
            .idCadenaPedido(ruta.getPedido().getT02IdCadena())
            .fechaPedido(ruta.getPedido().getT02FechaPedido())
            .cantidad(ruta.getPedido().getT02Cantidad())
            .destinoICAO(ruta.getPedido().getT02IdAeropDestino().getT01CodigoIcao())
            .destinoCiudad(ruta.getPedido().getT02IdAeropDestino().getT01Alias())
            .estado(ruta.getEstado().name())
            .cumpleSLA(ruta.getCumpleSla())
            .fechaEntregaEstimada(ruta.getFechaEntregaEstimada())
            .build();
    }

    /**
     * Convierte ruta a DTO completo
     */
    private RutaCompletaDTO convertirARutaCompleta(T08RutaPlaneada ruta) {
        List<TramoDTO> tramos = ruta.getTramosAsignados().stream()
            .map(this::convertirATramoDTO)
            .collect(Collectors.toList());

        return RutaCompletaDTO.builder()
            .idRuta(ruta.getId())
            .idPedido(ruta.getPedido().getId())
            .estadoRuta(ruta.getEstado().name())
            .cumpleSLA(ruta.getCumpleSla())
            .fechaEntregaEstimada(ruta.getFechaEntregaEstimada())
            .tramos(tramos)
            .build();
    }

    /**
     * Convierte tramo a DTO
     */
    private TramoDTO convertirATramoDTO(T09TramoAsignado tramo) {
        return TramoDTO.builder()
            .ordenEnRuta(tramo.getOrdenEnRuta())
            .esVueloFinal(tramo.getEsVueloFinal())
            .idVuelo(tramo.getVueloProgramado().getId())
            .origenICAO(tramo.getVueloProgramado().getT01IdAeropuertoOrigen().getT01CodigoIcao())
            .destinoICAO(tramo.getVueloProgramado().getT01IdAeropuertoDestino().getT01CodigoIcao())
            .fechaSalida(tramo.getVueloProgramado().getT04FechaSalida())
            .fechaLlegada(tramo.getVueloProgramado().getT04FechaLlegada())
            .cantidadProductos(tramo.getCantidadProductos())
            .build();
    }
}
