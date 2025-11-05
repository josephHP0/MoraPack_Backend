package com.morapack.service;

import com.morapack.dto.*;
import com.morapack.model.*;
import com.morapack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de planificación de pedidos.
 * Implementa el algoritmo de búsqueda de rutas y asignación de vuelos.
 */
@Service
public class OrderPlanningService {

    @Autowired
    private T03PedidoRepository pedidoRepository;

    @Autowired
    private T06VueloProgramadoRepository vueloRepository;

    @Autowired
    private T01AeropuertoRepository aeropuertoRepository;

    private static final long MINIMUM_CONNECTION_TIME_MINUTES = 60; // 1 hora
    private static final int SLA_SAME_CONTINENT_DAYS = 2;
    private static final int SLA_DIFFERENT_CONTINENT_DAYS = 3;
    private static final DateTimeFormatter INSTANCE_ID_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:00.000'Z'");

    /**
     * Representa una instancia de vuelo con su capacidad disponible.
     */
    private static class FlightInstance {
        String instanceId;
        String flightId;
        String origin;
        String dest;
        ZonedDateTime departure;
        ZonedDateTime arrival;
        int capacity;
        int availableCapacity;

        FlightInstance(T06VueloProgramado vuelo, LocalDate date, int dayOffset) {
            T01Aeropuerto origen = vuelo.getT01Idaeropuertoorigen();
            T01Aeropuerto destino = vuelo.getT01Idaeropuertodestino();

            ZoneOffset offsetOrigen = ZoneOffset.ofHours(origen.getT01GmtOffset());
            ZoneOffset offsetDestino = ZoneOffset.ofHours(destino.getT01GmtOffset());

            ZonedDateTime baseDeparture = vuelo.getT06Fechasalida().atZone(offsetOrigen);
            ZonedDateTime baseArrival = vuelo.getT06Fechallegada().atZone(offsetDestino);

            this.departure = baseDeparture
                .withDayOfMonth(date.getDayOfMonth())
                .withMonth(date.getMonthValue())
                .withYear(date.getYear());

            long durationSeconds = ChronoUnit.SECONDS.between(baseDeparture, baseArrival);
            this.arrival = this.departure.plusSeconds(durationSeconds);

            this.flightId = String.format("MP-%03d", vuelo.getId());
            String depTimeForInstanceId = this.departure.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            this.instanceId = String.format("%s#%s:00.000Z", this.flightId, depTimeForInstanceId);

            this.origin = origen.getT01Codigoicao();
            this.dest = destino.getT01Codigoicao();
            this.capacity = vuelo.getT06Capacidadtotal();
            this.availableCapacity = this.capacity;
        }
    }

    /**
     * Representa un camino (ruta) con múltiples vuelos.
     */
    private static class Path {
        List<FlightInstance> flights;
        int minCapacity;
        ZonedDateTime finalArrival;

        Path() {
            this.flights = new ArrayList<>();
            this.minCapacity = Integer.MAX_VALUE;
        }

        Path(Path other) {
            this.flights = new ArrayList<>(other.flights);
            this.minCapacity = other.minCapacity;
            this.finalArrival = other.finalArrival;
        }

        void addFlight(FlightInstance flight) {
            flights.add(flight);
            minCapacity = Math.min(minCapacity, flight.availableCapacity);
            finalArrival = flight.arrival;
        }
    }

    /**
     * Planifica todos los pedidos pendientes.
     */
    @Transactional(readOnly = true)
    public List<OrderPlanDTO> planificarPedidos() {
        // Obtener pedidos pendientes
        List<T03Pedido> pedidos = pedidoRepository.findAll().stream()
            .filter(p -> "PENDIENTE".equals(p.getT03Estadoglobal()))
            .collect(Collectors.toList());

        // Generar instancias de vuelo para la próxima semana
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC);
        Map<String, List<FlightInstance>> flightsByOrigin = generarInstanciasVuelo(startDate, 7);

        // Planificar cada pedido
        List<OrderPlanDTO> result = new ArrayList<>();

        for (T03Pedido pedido : pedidos) {
            OrderPlanDTO orderPlan = planificarPedido(pedido, flightsByOrigin, startDate);
            if (orderPlan != null && !orderPlan.getSplits().isEmpty()) {
                result.add(orderPlan);
            }
        }

        return result;
    }

    /**
     * Genera instancias de vuelo para un número de días.
     */
    private Map<String, List<FlightInstance>> generarInstanciasVuelo(LocalDate startDate, int days) {
        List<T06VueloProgramado> vuelosBase = vueloRepository.findAll();
        Map<String, List<FlightInstance>> flightsByOrigin = new HashMap<>();

        for (int dayOffset = 0; dayOffset < days; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);

            for (T06VueloProgramado vuelo : vuelosBase) {
                FlightInstance instance = new FlightInstance(vuelo, currentDate, dayOffset);

                flightsByOrigin
                    .computeIfAbsent(instance.origin, k -> new ArrayList<>())
                    .add(instance);
            }
        }

        // Ordenar por hora de salida
        for (List<FlightInstance> flights : flightsByOrigin.values()) {
            flights.sort(Comparator.comparing(f -> f.departure));
        }

        return flightsByOrigin;
    }

    /**
     * Planifica un pedido individual.
     */
    private OrderPlanDTO planificarPedido(
            T03Pedido pedido,
            Map<String, List<FlightInstance>> flightsByOrigin,
            LocalDate startDate) {

        String origen = pedido.getT01Idaeropuertoorigen().getT01Codigoicao();
        String destino = pedido.getT01Idaeropuertodestino().getT01Codigoicao();
        int cantidad = pedido.getT03Cantidadpaquetes();

        // Determinar SLA
        int slaDays = calcularSLA(
            pedido.getT01Idaeropuertoorigen(),
            pedido.getT01Idaeropuertodestino()
        );

        ZonedDateTime orderTime = pedido.getT03Fechacreacion().atZone(ZoneOffset.UTC);
        ZonedDateTime slaDeadline = orderTime.plusDays(slaDays);

        // Buscar rutas viables
        List<Path> rutas = buscarRutas(
            origen,
            destino,
            orderTime,
            slaDeadline,
            flightsByOrigin
        );

        if (rutas.isEmpty()) {
            return null; // No se encontró ruta
        }

        // Asignar vuelos a splits
        OrderPlanDTO orderPlan = new OrderPlanDTO();
        orderPlan.setOrderId("ORD-" + String.format("%03d", pedido.getId()));
        orderPlan.setSplits(new ArrayList<>());

        int remainingQty = cantidad;
        int splitIndex = 0;
        char splitLetter = 'A';

        for (Path ruta : rutas) {
            if (remainingQty <= 0) break;

            int qtyToAssign = Math.min(remainingQty, ruta.minCapacity);

            if (qtyToAssign > 0) {
                SplitDTO split = new SplitDTO();

                // Generar consignmentId
                if (cantidad <= ruta.minCapacity) {
                    // Un solo split
                    split.setConsignmentId("C-" + String.format("%03d", pedido.getId()) + "-1");
                } else {
                    // Múltiples splits
                    split.setConsignmentId("C-" + String.format("%03d", pedido.getId()) + "-" + splitLetter);
                    splitLetter++;
                }

                split.setQty(qtyToAssign);
                split.setLegs(new ArrayList<>());

                // Agregar legs
                int seq = 1;
                for (FlightInstance flight : ruta.flights) {
                    LegDTO leg = new LegDTO();
                    leg.setSeq(seq++);
                    leg.setInstanceId(flight.instanceId);
                    leg.setFrom(flight.origin);
                    leg.setTo(flight.dest);
                    leg.setQty(qtyToAssign);

                    split.getLegs().add(leg);

                    // Reducir capacidad disponible
                    flight.availableCapacity -= qtyToAssign;
                }

                orderPlan.getSplits().add(split);
                remainingQty -= qtyToAssign;
            }
        }

        return orderPlan;
    }

    /**
     * Busca rutas viables usando BFS.
     */
    private List<Path> buscarRutas(
            String origen,
            String destino,
            ZonedDateTime startTime,
            ZonedDateTime deadline,
            Map<String, List<FlightInstance>> flightsByOrigin) {

        List<Path> rutasCompletas = new ArrayList<>();
        Queue<Path> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Inicializar con vuelos desde el origen
        Path initialPath = new Path();
        queue.add(initialPath);
        visited.add(origen + "|" + startTime);

        while (!queue.isEmpty() && rutasCompletas.size() < 5) { // Máximo 5 rutas
            Path currentPath = queue.poll();

            String currentAirport = currentPath.flights.isEmpty() ?
                origen : currentPath.flights.get(currentPath.flights.size() - 1).dest;

            ZonedDateTime earliestDeparture = currentPath.flights.isEmpty() ?
                startTime : currentPath.finalArrival.plusMinutes(MINIMUM_CONNECTION_TIME_MINUTES);

            // Obtener vuelos desde el aeropuerto actual
            List<FlightInstance> availableFlights = flightsByOrigin.getOrDefault(currentAirport, new ArrayList<>());

            for (FlightInstance flight : availableFlights) {
                // Verificar que el vuelo sea después del tiempo mínimo de conexión
                if (flight.departure.isBefore(earliestDeparture)) {
                    continue;
                }

                // Verificar que llegue antes del deadline
                if (flight.arrival.isAfter(deadline)) {
                    continue;
                }

                // Verificar que tenga capacidad
                if (flight.availableCapacity <= 0) {
                    continue;
                }

                // Crear nuevo camino
                Path newPath = new Path(currentPath);
                newPath.addFlight(flight);

                // Si llegamos al destino, agregar a rutas completas
                if (flight.dest.equals(destino)) {
                    rutasCompletas.add(newPath);
                    continue;
                }

                // Evitar ciclos
                String visitKey = flight.dest + "|" + flight.arrival;
                if (!visited.contains(visitKey) && newPath.flights.size() < 3) { // Máximo 3 vuelos
                    visited.add(visitKey);
                    queue.add(newPath);
                }
            }
        }

        // Ordenar por capacidad mínima (mayor primero)
        rutasCompletas.sort((a, b) -> Integer.compare(b.minCapacity, a.minCapacity));

        return rutasCompletas;
    }

    /**
     * Calcula el SLA en días según los continentes.
     */
    private int calcularSLA(T01Aeropuerto origen, T01Aeropuerto destino) {
        String continenteOrigen = origen.getT08Idciudad().getT08Continente();
        String continenteDestino = destino.getT08Idciudad().getT08Continente();

        if (continenteOrigen != null && continenteDestino != null &&
            continenteOrigen.equalsIgnoreCase(continenteDestino)) {
            return SLA_SAME_CONTINENT_DAYS;
        } else {
            return SLA_DIFFERENT_CONTINENT_DAYS;
        }
    }
}
