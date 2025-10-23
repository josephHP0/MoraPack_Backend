package com.morapack.nucleo;

import com.morapack.adapter.AeropuertoAdapter;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import com.morapack.nucleo.GrafoVuelos.FlightInstance;
import com.morapack.simulador.CancellationRecord;
import com.morapack.service.EstadosTemporales;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orquestador principal que maneja la planificación de vuelos:
 * - Compila plan de vuelos
 * - Construye timeline de eventos
 * - Maneja estados y cancelaciones
 * - Realiza replanificación cuando es necesario
 */
public class AppPlanificador {

    private final Map<String, T01Aeropuerto> aeropuertos;
    private final Map<String, AeropuertoAdapter> aeropuertoAdapters = new HashMap<>();
    private final GrafoVuelos grafo;
    private final PlanificadorAco planner;
    private final com.morapack.simulador.TimelineStore store = new com.morapack.simulador.TimelineStore();

    // Última ventana de tiempo para reconstrucción de capacidades
    private Instant lastStart;
    private Instant lastEnd;

    public AppPlanificador(Map<String, T01Aeropuerto> aeropuertos, GrafoVuelos grafo) {
        this.aeropuertos = aeropuertos;
        this.grafo = grafo;
        
        // Crear adaptadores para cada aeropuerto
        aeropuertos.forEach((key, value) -> 
            aeropuertoAdapters.put(key, new AeropuertoAdapter(value))
        );
        
        // El tipo 'Aeropuerto' estaba faltando en el proyecto; hacer un casteo sin comprobación
        // para mantener compatibilidad con el constructor existente de PlanificadorAco.
        this.planner = new PlanificadorAco(grafo, (Map) aeropuertoAdapters);
    }

    /** Compilar plan semanal y publicar timeline */
    public String compileWeekly(Instant tStart, Instant tEnd, List<T03Pedido> pedidos, YearMonth periodo) {
        this.lastStart = tStart;
        this.lastEnd = tEnd;

        com.morapack.simulador.PlanResult plan = planner.compilarPlanSemanal(tStart, tEnd, pedidos, periodo);
        List<com.morapack.simulador.Event> timeline = buildTimeline(plan.getAsignaciones());
        store.load(plan, timeline);
        return store.getVersion();
    }
    
    /** Construye la línea de tiempo a partir de las asignaciones */
    private List<com.morapack.simulador.Event> buildTimeline(List<com.morapack.model.T09Asignacion> asignaciones) {
        List<com.morapack.simulador.Event> events = new ArrayList<>();
        
        for (com.morapack.model.T09Asignacion asig : asignaciones) {
            if (asig.getRuta() != null && asig.getRuta().getTramos() != null) {
                for (com.morapack.nucleo.Ruta.Tramo tramo : asig.getRuta().getTramos()) {
                    // Evento de salida
                    events.add(new com.morapack.simulador.Event(
                        asig.getPedido().getId().toString(),
                        tramo.getInstanceId(),
                        "DEPARTURE",
                        tramo.getDepUtc(),
                        asig.getPaquetesAsignados(),
                        tramo.getOrigen(),
                        tramo.getDestino()
                    ));
                    
                    // Evento de llegada
                    events.add(new com.morapack.simulador.Event(
                        asig.getPedido().getId().toString(),
                        tramo.getInstanceId(),
                        "ARRIVAL",
                        tramo.getArrUtc(),
                        asig.getPaquetesAsignados(),
                        tramo.getOrigen(),
                        tramo.getDestino()
                    ));
                }
            }
        }
        
        events.sort((a, b) -> a.getTime().compareTo(b.getTime()));
        return events;
    }

    /** Capacidades por instancia para la última ventana. */
    private Map<String,Integer> buildInstanceCapacities() {
        if (lastStart == null || lastEnd == null) return Collections.emptyMap();
        List<FlightInstance> slots = grafo.expandirSlots(lastStart, lastEnd, aeropuertos);
        return slots.stream().collect(Collectors.toMap(fi -> fi.instanceId, fi -> fi.capacidad, (a,b)->a));
    }

    /** 2) Consultar estado en T (para el front) con colapso si ocurre. */
    public com.morapack.planificador.simulation.EstadosTemporales stateAt(Instant at) {
        Map<String,Integer> instCaps = buildInstanceCapacities();
        SnapshotService snapshotService = new SnapshotServiceInMemory(store.getEvents(), aeropuertos, instCaps);
        return snapshotService.stateAt(at);
    }

    /** 3) Slice de eventos para animación */
    public List<Event> eventsBetween(Instant from, Instant to) {
        return store.eventsBetween(from, to);
    }

    /** 4) Aplicar cancelaciones; devuelve pedidos afectados y nueva versión */
    public CancellationService.CancellationSummary applyCancelations(List<CancellationRecord> records) {
        CancellationService cancellationService = new CancellationServiceInMemory(store);
        CancellationService.CancellationSummary sum = cancellationService.apply(records);
        return sum;
    }

    /** 5) Replanificar pendientes y actualizar timeline (versiona) */
    public ReplannerService.Summary replanPending(Instant now, YearMonth periodo) {
        ReplannerService replannerService = new ReplannerServiceInMemory(store, grafo, aeropuertos);
        ReplannerService.Summary s = replannerService.replanPending(now, periodo);
        return s;
    }
}
