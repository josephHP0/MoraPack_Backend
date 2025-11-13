package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.repository.VueloProgramadoRepository;
import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;
import com.morapack.nuevomoraback.planificacion.domain.T09TramoAsignado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcoPlannerImpl implements AcoPlanner {

    private final AcoParameters params;
    private final HeuristicCalculator heuristicCalculator;
    private final PheromoneMatrix pheromoneMatrix;
    private final VueloProgramadoRepository vueloRepository;
    private final Random random = new Random();

    @Override
    public List<T08RutaPlaneada> planificar(List<T02Pedido> pedidos,
                                             T08RutaPlaneada.TipoSimulacion tipoSimulacion) {

        log.info("Iniciando planificación ACO para {} pedidos - Tipo: {}", pedidos.size(), tipoSimulacion);

        pheromoneMatrix.inicializar();

        Ant mejorHormiga = null;
        double mejorCosto = Double.MAX_VALUE;

        // Iteraciones del algoritmo ACO
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

        // Convertir mejor solución a entidades T08RutaPlaneada
        return convertirSolucion(mejorHormiga, tipoSimulacion);
    }

    private Ant construirSolucion(List<T02Pedido> pedidos) {
        Ant hormiga = new Ant();

        for (T02Pedido pedido : pedidos) {
            // Obtener vuelos candidatos (simplificado: todos los vuelos disponibles)
            List<T04VueloProgramado> vuelosCandidatos = obtenerVuelosCandidatos(pedido);

            if (vuelosCandidatos.isEmpty()) {
                continue; // Pedido no puede ser asignado
            }

            // Seleccionar vuelo usando probabilidad basada en feromona y heurística
            T04VueloProgramado vueloSeleccionado = seleccionarVuelo(pedido, vuelosCandidatos);
            hormiga.asignarVuelo(pedido, vueloSeleccionado);
        }

        return hormiga;
    }

    private List<T04VueloProgramado> obtenerVuelosCandidatos(T02Pedido pedido) {
        // Simplificado: en implementación real, filtrar por ruta hacia destino, capacidad, etc.
        Instant fechaMinima = pedido.getT02FechaPedido();
        Instant fechaMaxima = fechaMinima.plusSeconds(7 * 24 * 3600); // 7 días

        return vueloRepository.findVuelosDisponibles(fechaMinima, fechaMaxima);
    }

    private T04VueloProgramado seleccionarVuelo(T02Pedido pedido, List<T04VueloProgramado> candidatos) {

        double[] probabilidades = new double[candidatos.size()];
        double suma = 0.0;

        for (int i = 0; i < candidatos.size(); i++) {
            T04VueloProgramado vuelo = candidatos.get(i);

            double feromona = pheromoneMatrix.obtenerFeromona(pedido.getId(), vuelo.getId());
            double heuristica = heuristicCalculator.calcularHeuristica(pedido, vuelo, Instant.now());

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
