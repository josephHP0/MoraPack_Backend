package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.repository.VueloProgramadoRepository;
import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import com.morapack.nuevomoraback.planificacion.domain.T09TramoAsignado;
import com.morapack.nuevomoraback.planificacion.service.VueloExpansionService;
import com.morapack.nuevomoraback.planificacion.service.VueloExpansionService.VueloVirtual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcoPlannerImpl implements AcoPlanner {

    private final AcoParameters params;
    private final HeuristicCalculator heuristicCalculator;
    private final PheromoneMatrix pheromoneMatrix;
    private final VueloProgramadoRepository vueloRepository;
    private final VueloExpansionService vueloExpansionService;
    private final Random random = new Random();

    // Cache de vuelos expandidos para la simulación actual
    private List<VueloVirtual> vuelosExpandidosCache;

    @Override
    public List<T08RutaPlaneada> planificar(List<T02Pedido> pedidos,
                                             T08RutaPlaneada.TipoSimulacion tipoSimulacion,
                                             Instant fechaInicio,
                                             Instant fechaFin) {

        log.info("========================================");
        log.info("Iniciando planificación ACO");
        log.info("Pedidos a planificar: {}", pedidos.size());
        log.info("Tipo simulación: {}", tipoSimulacion);
        log.info("Periodo: {} a {}", fechaInicio, fechaFin);
        log.info("========================================");

        // 1. Expandir vuelos plantilla para todo el periodo de simulación
        List<T04VueloProgramado> vuelosPlantilla = vueloRepository.findAllVuelosPlantilla();
        log.info("✓ Cargados {} vuelos plantilla desde BD", vuelosPlantilla.size());

        if (vuelosPlantilla.isEmpty()) {
            log.error("ERROR: No hay vuelos plantilla en la BD. Verificar tabla T04_VUELO_PROGRAMADO");
            return new ArrayList<>();
        }

        vuelosExpandidosCache = vueloExpansionService.expandirVuelos(vuelosPlantilla, fechaInicio, fechaFin);
        log.info("✓ Vuelos expandidos: {} vuelos virtuales generados", vuelosExpandidosCache.size());

        if (pedidos.isEmpty()) {
            log.warn("ADVERTENCIA: No hay pedidos para planificar. Verificar query findPedidosNoHubBetween");
            return new ArrayList<>();
        }

        // 2. Inicializar matriz de feromonas
        pheromoneMatrix.inicializar();

        Ant mejorHormiga = null;
        double mejorCosto = Double.MAX_VALUE;

        // 3. Iteraciones del algoritmo ACO
        for (int iter = 0; iter < params.getNumeroIteraciones(); iter++) {
            List<Ant> hormigas = new ArrayList<>();

            // Construir soluciones con cada hormiga
            for (int h = 0; h < params.getNumeroHormigas(); h++) {
                Ant hormiga = construirSolucion(pedidos);
                hormiga.calcularCosto();
                hormigas.add(hormiga);

                if (hormiga.getCostoTotal() < mejorCosto) {
                    mejorCosto = hormiga.getCostoTotal();
                    mejorHormiga = hormiga;
                }
            }

            // Actualizar feromonas
            actualizarFeromonas(hormigas);
            pheromoneMatrix.evaporar();

            if (iter % 10 == 0) {
                log.debug("Iteración {}: Mejor costo = {}", iter, mejorCosto);
            }
        }

        // 4. Convertir mejor solución a entidades T08RutaPlaneada
        if (mejorHormiga == null || mejorHormiga.getSolucion().isEmpty()) {
            log.warn("ADVERTENCIA: No se encontró ninguna solución válida. Ningún pedido pudo ser asignado a vuelos.");
            return new ArrayList<>();
        }

        log.info("========================================");
        log.info("✓ ACO completado");
        log.info("Mejor solución: {} pedidos planificados", mejorHormiga.getSolucion().size());
        log.info("Costo total: {}", mejorCosto);
        log.info("========================================");

        return convertirSolucion(mejorHormiga, tipoSimulacion);
    }

    private Ant construirSolucion(List<T02Pedido> pedidos) {
        Ant hormiga = new Ant();
        int asignados = 0;
        int sinCandidatos = 0;

        for (T02Pedido pedido : pedidos) {
            // Obtener vuelos candidatos desde la caché de vuelos expandidos
            List<VueloVirtual> vuelosCandidatos = obtenerVuelosCandidatos(pedido);

            if (vuelosCandidatos.isEmpty()) {
                sinCandidatos++;
                continue; // Pedido no puede ser asignado
            }

            // Seleccionar vuelo usando probabilidad basada en feromona y heurística
            VueloVirtual vueloSeleccionado = seleccionarVuelo(pedido, vuelosCandidatos);
            hormiga.asignarVuelo(pedido, vueloSeleccionado.toEntity());
            asignados++;
        }

        log.trace("Hormiga construida: {} pedidos asignados, {} sin candidatos", asignados, sinCandidatos);

        return hormiga;
    }

    private List<VueloVirtual> obtenerVuelosCandidatos(T02Pedido pedido) {
        // Filtrar vuelos virtuales que sean viables para este pedido

        // IMPORTANTE: Usamos el DÍA del pedido, no la hora exacta
        // Esto permite que todos los vuelos del mismo día o posteriores sean candidatos
        LocalDate diaPedido = pedido.getT02FechaPedido().atZone(ZoneId.of("UTC")).toLocalDate();
        Instant fechaMinima = diaPedido.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant fechaMaxima = diaPedido.plusDays(7).atStartOfDay(ZoneId.of("UTC")).toInstant();

        List<VueloVirtual> candidatos = vuelosExpandidosCache.stream()
            .filter(v -> !v.getFechaSalida().isBefore(fechaMinima)) // >= fecha mínima (mismo día o después)
            .filter(v -> v.getFechaSalida().isBefore(fechaMaxima))  // < fecha máxima (dentro de 7 días)
            .filter(v -> v.getCapacidadDisponible() >= pedido.getT02Cantidad())
            .filter(v -> !"CANCELADO".equals(v.getEstado()))
            .collect(Collectors.toList());

        if (candidatos.isEmpty()) {
            log.debug("Pedido {}: No se encontraron vuelos candidatos (dia={}, cantidad={})",
                     pedido.getId(), diaPedido, pedido.getT02Cantidad());
        } else {
            log.trace("Pedido {}: {} vuelos candidatos encontrados", pedido.getId(), candidatos.size());
        }

        return candidatos;
    }

    private VueloVirtual seleccionarVuelo(T02Pedido pedido, List<VueloVirtual> candidatos) {

        double[] probabilidades = new double[candidatos.size()];
        double suma = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            VueloVirtual vuelo = candidatos.get(i);

            // Usar plantillaId para la feromona (múltiples vuelos virtuales del mismo vuelo base)
            double feromona = pheromoneMatrix.obtenerFeromona(pedido.getId(), vuelo.getPlantillaId());
            double heuristica = heuristicCalculator.calcularHeuristica(pedido, vuelo.toEntity(), vuelo.getFechaSalida());

            probabilidades[i] = Math.pow(feromona, params.getAlpha()) *
                               Math.pow(heuristica, params.getBeta());
            suma += probabilidades[i];
        }

        // Normalizar y seleccionar
        if (suma == 0.0) {
            return candidatos.get(random.nextInt(candidatos.size()));
        }

        double rand = random.nextDouble() * suma;
        double acumulado = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            acumulado += probabilidades[i];
            if (rand <= acumulado) {
                return candidatos.get(i);
            }
        }

        return candidatos.get(candidatos.size() - 1);
    }

    private void actualizarFeromonas(List<Ant> hormigas) {
        for (Ant hormiga : hormigas) {
            double deposito = 1.0 / (hormiga.getCostoTotal() + 1.0);

            hormiga.getSolucion().forEach((pedido, vuelos) -> {
                vuelos.forEach(vuelo -> {
                    pheromoneMatrix.actualizarFeromona(pedido.getId(), vuelo.getId(), deposito);
                });
            });
        }
    }

    private List<T08RutaPlaneada> convertirSolucion(Ant mejorHormiga,
                                                     T08RutaPlaneada.TipoSimulacion tipoSimulacion) {
        List<T08RutaPlaneada> rutas = new ArrayList<>();

        if (mejorHormiga == null) {
            return rutas;
        }

        mejorHormiga.getSolucion().forEach((pedido, vuelos) -> {
            T08RutaPlaneada ruta = new T08RutaPlaneada();
            ruta.setPedido(pedido);
            ruta.setFechaPlanificacion(Instant.now());
            ruta.setTipoSimulacion(tipoSimulacion);
            ruta.setEstado(T08RutaPlaneada.EstadoRuta.PENDIENTE);

            List<T09TramoAsignado> tramos = new ArrayList<>();
            for (int i = 0; i < vuelos.size(); i++) {
                T09TramoAsignado tramo = new T09TramoAsignado();
                tramo.setRutaPlaneada(ruta);
                tramo.setVueloProgramado(vuelos.get(i));
                tramo.setCantidadProductos(pedido.getT02Cantidad());
                tramo.setOrdenEnRuta((short) (i + 1));
                tramo.setEsVueloFinal(i == vuelos.size() - 1);
                tramos.add(tramo);
            }

            ruta.setTramosAsignados(tramos);
            rutas.add(ruta);
        });

        return rutas;
    }
}
