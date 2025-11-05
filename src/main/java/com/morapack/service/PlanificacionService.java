package com.morapack.service;

import com.morapack.dto.PlanificacionRespuestaDTO;
import com.morapack.dto.PlanificacionSemanalDTO;
import com.morapack.model.*;
import com.morapack.nucleo.*;
import com.morapack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PlanificacionService {
    
    @Autowired
    private T03PedidoRepository pedidoRepository;
    
    @Autowired
    private T06VueloProgramadoRepository vueloRepository;
    
    @Autowired
    private T01AeropuertoRepository aeropuertoRepository;

    @Transactional
    public PlanificacionSemanalDTO planificarSemanal(Instant fechaInicio, Instant fechaFin) {
        // 1. Cargar datos necesarios
        List<T03Pedido> pedidos = pedidoRepository.findByT03FechacreacionBetween(fechaInicio, fechaFin);
        List<T06VueloProgramado> vuelos = vueloRepository.findByT06FechasalidaBetween(fechaInicio, fechaFin);
        List<T01Aeropuerto> aeropuertos = aeropuertoRepository.findAll();

        // 2. Construir grafo de vuelos y mapa de aeropuertos
        GrafoVuelos grafo = new GrafoVuelos();
        Map<String, T01Aeropuerto> mapaAeropuertos = new HashMap<>();
        
        for (T01Aeropuerto ap : aeropuertos) {
            grafo.agregarAeropuerto(ap);
            mapaAeropuertos.put(ap.getT01Codigoicao(), ap);
        }
        for (T06VueloProgramado vuelo : vuelos) {
            grafo.agregarVuelo(vuelo);
        }

        // 3. Configurar parámetros ACO
        ParametrosAco parametros = ParametrosAco.defaults();

        // 4. Ejecutar planificación
        PlanificadorAco planificador = new PlanificadorAco(grafo, mapaAeropuertos, parametros);
        List<PlanificacionRespuestaDTO> solucion = planificador.planificarSemanal(pedidos, fechaInicio, fechaFin);

        // 5. Construir respuesta
        return construirRespuesta(solucion, pedidos);
    }

    private PlanificacionSemanalDTO construirRespuesta(List<PlanificacionRespuestaDTO> solucion, List<T03Pedido> pedidosTotales) {
        PlanificacionSemanalDTO dto = new PlanificacionSemanalDTO();
        List<PlanificacionSemanalDTO.RutaPedidoDTO> rutas = new ArrayList<>();
        Map<Integer, T03Pedido> mapaPedidos = new HashMap<>();
        
        // Indexar pedidos por ID
        for (T03Pedido pedido : pedidosTotales) {
            mapaPedidos.put(pedido.getId(), pedido);
        }
        
        // Convertir cada plan de ruta a DTO
        for (PlanificacionRespuestaDTO plan : solucion) {
            if (!"PLANIFICADO".equals(plan.getEstado())) continue;
            
            T03Pedido pedido = mapaPedidos.get(plan.getPedidoId());
            if (pedido == null) continue;
            
            PlanificacionSemanalDTO.RutaPedidoDTO rutaDto = new PlanificacionSemanalDTO.RutaPedidoDTO();
            rutaDto.setIdPedido(pedido.getId());
            rutaDto.setCantidadPaquetes(pedido.getT03Cantidadpaquetes());
            
            List<PlanificacionSemanalDTO.TramoDTO> tramos = new ArrayList<>();
            Map<String, T06VueloProgramado> vuelosRuta = new HashMap<>();
            
            // Obtener vuelos de la ruta
            for (String vueloId : plan.getVuelos()) {
                String[] parts = vueloId.split("#");
                if (parts.length != 2) continue;
                
                Integer id = Integer.parseInt(parts[0].substring(3));
                T06VueloProgramado vuelo = vueloRepository.findById(id).orElse(null);
                if (vuelo == null) continue;
                
                vuelosRuta.put(vueloId, vuelo);
                
                PlanificacionSemanalDTO.TramoDTO tramoDto = new PlanificacionSemanalDTO.TramoDTO();
                tramoDto.setIdVuelo(vuelo.getId());
                tramoDto.setAeropuertoOrigen(vuelo.getT01Idaeropuertoorigen().getT01Codigoicao());
                tramoDto.setAeropuertoDestino(vuelo.getT01Idaeropuertodestino().getT01Codigoicao());
                tramoDto.setFechaSalida(vuelo.getT06Fechasalida());
                tramoDto.setFechaLlegada(vuelo.getT06Fechallegada());
                tramoDto.setCantidadPaquetes(pedido.getT03Cantidadpaquetes());
                tramos.add(tramoDto);
            }
            
            rutaDto.setTramos(tramos);
            
            // Verificar cumplimiento de plazo
            if (!tramos.isEmpty()) {
                Instant ultimaLlegada = tramos.get(tramos.size() - 1).getFechaLlegada();
                rutaDto.setCumplePlazo(ultimaLlegada.isBefore(pedido.getT03Plazocompromiso()));
            }
            
            rutas.add(rutaDto);
        }
        dto.setRutas(rutas);
        
        // Calcular métricas
        PlanificacionSemanalDTO.MetricasDTO metricas = new PlanificacionSemanalDTO.MetricasDTO();
        metricas.setTotalPedidos(pedidosTotales.size());
        metricas.setPedidosPlanificados((int)solucion.stream()
            .filter(p -> "PLANIFICADO".equals(p.getEstado()))
            .count());
        metricas.setTotalPaquetes(pedidosTotales.stream()
            .mapToInt(T03Pedido::getT03Cantidadpaquetes)
            .sum());
        metricas.setPaquetesPlanificados(rutas.stream()
            .mapToInt(PlanificacionSemanalDTO.RutaPedidoDTO::getCantidadPaquetes)
            .sum());
        metricas.setTasaCumplimiento((double) metricas.getPedidosPlanificados() / metricas.getTotalPedidos());
        metricas.setUtilizacionPromedio(0.0); // TODO: Calcular utilización promedio
        metricas.setTiempoPromedioEntrega(0.0); // TODO: Calcular tiempo promedio de entrega
        
        dto.setMetricas(metricas);
        return dto;
    }
}