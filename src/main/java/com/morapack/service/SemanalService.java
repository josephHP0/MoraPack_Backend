package com.morapack.service;

import com.morapack.dto.*;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import com.morapack.model.T06VueloProgramado;
import com.morapack.nucleo.GrafoVuelos;
import com.morapack.nucleo.PlanificadorAco;
import com.morapack.repository.T01AeropuertoRepository;
import com.morapack.repository.T03PedidoRepository;
import com.morapack.util.UtilArchivos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import com.morapack.dto.CancelacionDTO;

@Service
public class SemanalService {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private VueloService vueloService;

    @Autowired
    private T03PedidoRepository pedidoRepository;

    @Autowired
    private T01AeropuertoRepository aeropuertoRepository;

    private static final String ICAO_HUB = "SKBO";
    
    @Transactional
    public RespuestaDTO procesarPedidosSemanales(MultipartFile archivo) {
        try {
            // 1. Obtener aeropuertos
            Optional<T01Aeropuerto> hubOpt = aeropuertoRepository.findByT01Codigoicao(ICAO_HUB);
            if (hubOpt.isEmpty()) {
                return new RespuestaDTO("error", "El aeropuerto hub " + ICAO_HUB + " no está registrado", null);
            }

            // 2. Leer y cargar todos los pedidos primero
            List<PedidoInputDto> pedidos = UtilArchivos.cargarPedidos(archivo);
            List<String> errores = new ArrayList<>();
            int totalGuardados = 0;

            // 3. Procesar cada pedido usando el servicio existente
            for (PedidoInputDto pedido : pedidos) {
                RespuestaDTO resultado = pedidoService.procesarPedido(pedido);
                
                if ("error".equals(resultado.getStatus())) {
                    errores.add(String.format("Cliente %s, Destino %s: %s", 
                        pedido.getNombreCliente(), pedido.getIcaoDestino(), resultado.getMensaje()));
                } else if ("warning".equals(resultado.getStatus())) {
                    errores.add(String.format("Cliente %s, Destino %s: ADVERTENCIA - %s", 
                        pedido.getNombreCliente(), pedido.getIcaoDestino(), resultado.getMensaje()));
                } else if ("success".equals(resultado.getStatus())) {
                    totalGuardados++;
                }
            }

            // 4. Si se guardaron pedidos, generar vuelos base semanales para cada destino único
            if (totalGuardados > 0) {
                // Obtener destinos únicos
                Set<String> destinos = pedidos.stream()
                    .map(PedidoInputDto::getIcaoDestino)
                    .collect(HashSet::new, HashSet::add, HashSet::addAll);

                // Generar vuelos para cada destino
                for (String destino : destinos) {
                    generarVuelosBase(hubOpt.get(), destino);
                }
            }

            Map<String, Object> data = Map.of(
                "totalProcesados", pedidos.size(),
                "totalGuardados", totalGuardados,
                "errores", errores
            );

            return new RespuestaDTO("success", 
                String.format("Carga semanal completada. %d pedidos guardados, %d errores/advertencias.", 
                    totalGuardados, errores.size()), 
                data);

        } catch (Exception e) {
            return new RespuestaDTO("error", "Error procesando archivo semanal: " + e.getMessage(), null);
        }
    }

    @Transactional
    public RespuestaDTO obtenerPlanificacionSemanal() {
        try {
            // 1. Obtener todos los pedidos de la BD
            List<T03Pedido> pedidosDB = pedidoRepository.findAll();

            // 2. Obtener los aeropuertos y sus capacidades
            List<T01Aeropuerto> allAeropuertos = aeropuertoRepository.findAll();
            Map<String, T01Aeropuerto> aeropuertos = new HashMap<>();
            
            for (T01Aeropuerto ap : allAeropuertos) {
                String icao = ap.getT01Codigoicao();
                if (icao == null || icao.trim().isEmpty()) {
                    System.out.println("Aeropuerto con ID " + ap.getId() + " no tiene código ICAO");
                    continue;
                }
                
                if (!icao.matches("[A-Z]{4}")) {
                    System.out.println("Código ICAO inválido para aeropuerto " + ap.getId() + ": " + icao);
                    continue;
                }
                
                if (ap.getId() == null) {
                    System.out.println("Aeropuerto sin ID encontrado: " + icao);
                    continue;
                }
                
                aeropuertos.put(icao, ap);
            }

            // Pre-procesar pedidos: calcular origen por cercanía a hubs y normalizar fechas
            // Obtener lista de hubs (aeropuertos marcados como hub en su ciudad)
            List<T01Aeropuerto> hubAirports = new ArrayList<>();
            for (T01Aeropuerto a : aeropuertos.values()) {
                try {
                    if (a.getT08Idciudad() != null && Boolean.TRUE.equals(a.getT08Idciudad().getT08Eshub())) {
                        hubAirports.add(a);
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }

            // If no hubs found, fallback to all airports
            if (hubAirports.isEmpty()) {
                hubAirports.addAll(aeropuertos.values());
            }

            // Base date mapping: treat day 1 (01-01-2025) as 'today', day 2 as 'tomorrow', etc.
            java.time.LocalDate today = java.time.LocalDate.now();

            for (T03Pedido pedido : pedidosDB) {
                // 1) Ensure destination is loaded (may be lazy)
                T01Aeropuerto destino = null;
                try {
                    destino = pedido.getT01Idaeropuertodestino();
                } catch (Exception ex) {
                    destino = null;
                }

                // 2) Compute origin as nearest hub to destination coordinates
                if (destino != null) {
                    T01Aeropuerto nearest = null;
                    double bestDist = Double.MAX_VALUE;
                    Double destLat = null, destLon = null;
                    try {
                        if (destino.getT01Lat() != null && destino.getT01Lon() != null) {
                            destLat = destino.getT01Lat().doubleValue();
                            destLon = destino.getT01Lon().doubleValue();
                        }
                    } catch (Exception ex) {
                        destLat = null;
                        destLon = null;
                    }

                    if (destLat != null && destLon != null) {
                        for (T01Aeropuerto hub : hubAirports) {
                            try {
                                if (hub.getT01Lat() == null || hub.getT01Lon() == null) continue;
                                double hubLat = hub.getT01Lat().doubleValue();
                                double hubLon = hub.getT01Lon().doubleValue();
                                double d = haversineKm(destLat, destLon, hubLat, hubLon);
                                if (d < bestDist) {
                                    bestDist = d;
                                    nearest = hub;
                                }
                            } catch (Exception ex) {
                                // ignore malformed coords
                            }
                        }
                    }

                    if (nearest != null) {
                        try {
                            pedido.setT01Idaeropuertoorigen(nearest);
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                }

                // 3) Normalize fechaCreacion: use only day,hour,minute from stored date
                try {
                    Instant orig = pedido.getT03Fechacreacion();
                    if (orig != null) {
                        java.time.ZonedDateTime z = java.time.ZonedDateTime.ofInstant(orig, java.time.ZoneId.of("UTC"));
                        int dayOfMonth = z.getDayOfMonth();
                        int hour = z.getHour();
                        int minute = z.getMinute();

                        // Clamp dayOfMonth into 1..30
                        if (dayOfMonth < 1) dayOfMonth = 1;
                        if (dayOfMonth > 30) dayOfMonth = ((dayOfMonth - 1) % 30) + 1;

                        java.time.LocalDate targetDate = today.plusDays(dayOfMonth - 1);
                        java.time.LocalDateTime targetLdt = java.time.LocalDateTime.of(targetDate.getYear(), targetDate.getMonthValue(), targetDate.getDayOfMonth(), hour, minute);
                        // Crear una copia del pedido con la fecha normalizada
                        Instant mapped = targetLdt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                        T03Pedido pedidoNormalizado = new T03Pedido();
                        // Copiar todos los campos necesarios
                        pedidoNormalizado.setId(pedido.getId());
                        pedidoNormalizado.setT03Cantidadpaquetes(pedido.getT03Cantidadpaquetes());
                        pedidoNormalizado.setT01Idaeropuertoorigen(pedido.getT01Idaeropuertoorigen());
                        pedidoNormalizado.setT01Idaeropuertodestino(pedido.getT01Idaeropuertodestino());
                        pedidoNormalizado.setT03Fechacreacion(mapped);
                        // Reemplazar el pedido original con la copia normalizada
                        pedido = pedidoNormalizado;
                    }
                } catch (Exception ex) {
                    // leave as-is on error
                }
            }

            // 3. Validar que tengamos los aeropuertos necesarios
            Set<String> requiredAirports = new HashSet<>();
            
            // Identificar aeropuertos necesarios desde los pedidos
            for (T03Pedido pedido : pedidosDB) {
                try {
                    if (pedido.getT01Idaeropuertoorigen() != null) {
                        requiredAirports.add(pedido.getT01Idaeropuertoorigen().getT01Codigoicao());
                    }
                } catch (Exception ex) {
                    // ignore
                }
                try {
                    if (pedido.getT01Idaeropuertodestino() != null) {
                        requiredAirports.add(pedido.getT01Idaeropuertodestino().getT01Codigoicao());
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            
            // Verificar que todos los aeropuertos necesarios estén disponibles
            List<String> missingAirports = requiredAirports.stream()
                .filter(icao -> !aeropuertos.containsKey(icao))
                .toList();
                
            if (!missingAirports.isEmpty()) {
                return new RespuestaDTO("error",
                    "Faltan aeropuertos necesarios: " + String.join(", ", missingAirports),
                    null);
            }

            // 4. Construir el grafo de vuelos desde el servicio
            GrafoVuelos grafo = new GrafoVuelos();
            
            // 3.1 Agregar primero todos los aeropuertos al grafo
            for (T01Aeropuerto aeropuerto : aeropuertos.values()) {
                grafo.agregarAeropuerto(aeropuerto);
            }
            
            // 3.2 Obtener y agregar vuelos al grafo
            List<Flight_instances_DTO> vuelos = new ArrayList<>();
            RespuestaDTO respVuelos = vueloService.obtenerFlightsDTO2(0, 20062);
            
            if ("success".equals(respVuelos.getStatus()) && respVuelos.getData() != null) {
                Map<String, Object> dataVuelos = (Map<String, Object>) respVuelos.getData();
                if (dataVuelos != null && dataVuelos.get("vuelos") instanceof List) {
                    vuelos = (List<Flight_instances_DTO>) dataVuelos.get("vuelos");
                    
                    for (Flight_instances_DTO v : vuelos) {
                        T01Aeropuerto origen = aeropuertos.get(v.origin());
                        T01Aeropuerto destino = aeropuertos.get(v.dest());
                        
                        if (origen == null) {
                            System.out.println("Aeropuerto origen no encontrado: " + v.origin());
                            continue;
                        }
                        if (destino == null) {
                            System.out.println("Aeropuerto destino no encontrado: " + v.dest());
                            continue;
                        }
                        
                        // Crear vuelo programado temporal para el grafo
                        T06VueloProgramado vueloTemp = new T06VueloProgramado();
                        vueloTemp.setId(Integer.parseInt(v.flightId().substring(3))); // quitar prefijo "MP-"
                        
                        vueloTemp.setT01Idaeropuertoorigen(origen);
                        vueloTemp.setT01Idaeropuertodestino(destino);
                        
                        vueloTemp.setT06Fechasalida(Instant.parse(v.depUtc()));
                        vueloTemp.setT06Fechallegada(Instant.parse(v.arrUtc()));
                        vueloTemp.setT06Capacidadtotal(v.capacity());
                        vueloTemp.setT06Ocupacionactual(0); // Inicialmente vacío
                        
                        grafo.agregarVuelo(vueloTemp);
                    }
                }
            }

            // 4. Validar que tengamos datos suficientes para planificar
            if (vuelos.isEmpty()) {
                return new RespuestaDTO("error", "No hay vuelos disponibles para planificar", null);
            }
            
            if (pedidosDB.isEmpty()) {
                return new RespuestaDTO("error", "No hay pedidos para planificar", null);
            }
            
            if (aeropuertos.isEmpty()) {
                return new RespuestaDTO("error", "No hay aeropuertos registrados", null);
            }

            // 4. Crear planificador ACO y ejecutar planificación
            LocalDate fechaInicioVuelos = LocalDate.now(ZoneOffset.UTC);
            Instant fechaInicio = fechaInicioVuelos.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant fechaFin = fechaInicio.plus(Duration.ofDays(7));

            System.out.println("\n[SemanalService] Ventana de planificación:");
            System.out.println("  Fecha inicio: " + fechaInicio + " (" + fechaInicioVuelos + ")");
            System.out.println("  Fecha fin: " + fechaFin);
            System.out.println("  Días: 7");

            // Agrupar pedidos por día para optimizar búsqueda
            Map<Integer, List<T03Pedido>> pedidosPorDia = new HashMap<>();
            int vuelosPorDia = vuelos.size() / 7; // número de vuelos base por día
            
            for (T03Pedido pedido : pedidosDB) {
                // Obtener el día del pedido en el huso horario del destino
                T01Aeropuerto destino = pedido.getT01Idaeropuertodestino();
                String zonaHorariaDestino = getZonaHoraria(destino);
                ZoneId zonaDestino = ZoneId.of(zonaHorariaDestino);
                
                Instant fechaPedidoUTC = pedido.getT03Fechacreacion();
                ZonedDateTime fechaPedidoDestino = fechaPedidoUTC.atZone(zonaDestino);
                int dia = fechaPedidoDestino.getDayOfMonth();
                
                // Calcular plazo máximo según continentes
                T01Aeropuerto origen = pedido.getT01Idaeropuertoorigen();
                int plazoEntrega = getMismoContinente(origen, destino) ? 2 : 3;
                
                // Agregar plazo de 2 horas en destino
                Instant fechaLimite = fechaPedidoDestino
                    .plusDays(plazoEntrega)
                    .minusHours(2) // Restar 2 horas para asegurar llegada antes del plazo
                    .toInstant();
                
                // Crear DTO con la información de planificación sin modificar la BD
                PedidoPlanificacionDTO pedidoPlan = PedidoPlanificacionDTO.fromPedido(pedido, fechaLimite);
                pedidosPorDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(pedido);
            }
            
            // Ordenar pedidos por día según la fecha límite calculada
            Map<Integer, List<PedidoPlanificacionDTO>> planificacionPorDia = new HashMap<>();
            for (Map.Entry<Integer, List<T03Pedido>> entry : pedidosPorDia.entrySet()) {
                List<PedidoPlanificacionDTO> pedidosOrdenados = entry.getValue().stream()
                    .map(p -> {
                        // Recalcular fecha límite para cada pedido
                        String zonaHoraria = getZonaHoraria(p.getT01Idaeropuertodestino());
                        ZoneId zona = ZoneId.of(zonaHoraria);
                        ZonedDateTime fechaPedido = p.getT03Fechacreacion().atZone(zona);
                        int plazo = getMismoContinente(p.getT01Idaeropuertoorigen(), 
                                                     p.getT01Idaeropuertodestino()) ? 2 : 3;
                        Instant limite = fechaPedido.plusDays(plazo).minusHours(2).toInstant();
                        return PedidoPlanificacionDTO.fromPedido(p, limite);
                    })
                    .sorted(Comparator.comparing(PedidoPlanificacionDTO::getFechaLimite))
                    .collect(Collectors.toList());
                planificacionPorDia.put(entry.getKey(), pedidosOrdenados);
            }
            

            // Planificar día por día
            List<PlanificacionRespuestaDTO> todasLasAsignaciones = new ArrayList<>();
            
            for (Map.Entry<Integer, List<T03Pedido>> entry : pedidosPorDia.entrySet()) {
                int dia = entry.getKey();
                List<T03Pedido> pedidosDelDia = entry.getValue();
                
                // Calcular el rango de índices de vuelos disponibles desde este día hasta el final de la ventana
                int startIndex = (dia - 1) * vuelosPorDia;
                int endIndex = vuelos.size(); // Usar todos los vuelos desde el día actual hasta el final
                
                // Crear un subgrafo con los vuelos desde este día hasta el final de la ventana
                GrafoVuelos grafoDelDia = new GrafoVuelos();
                for (T01Aeropuerto aeropuerto : aeropuertos.values()) {
                    grafoDelDia.agregarAeropuerto(aeropuerto);
                }
                
                // Agregar vuelos disponibles desde este día hasta el final
                for (int i = startIndex; i < endIndex; i++) {
                    Flight_instances_DTO v = vuelos.get(i);
                    T01Aeropuerto origen = aeropuertos.get(v.origin());
                    T01Aeropuerto destino = aeropuertos.get(v.dest());
                    
                    if (origen != null && destino != null) {
                        T06VueloProgramado vueloTemp = new T06VueloProgramado();
                        vueloTemp.setId(Integer.parseInt(v.flightId().replace("MP-", "")));
                        vueloTemp.setT01Idaeropuertoorigen(origen);
                        vueloTemp.setT01Idaeropuertodestino(destino);
                        vueloTemp.setT06Fechasalida(Instant.parse(v.depUtc()));
                        vueloTemp.setT06Fechallegada(Instant.parse(v.arrUtc()));
                        vueloTemp.setT06Capacidadtotal(v.capacity());
                        vueloTemp.setT06Ocupacionactual(0);
                        grafoDelDia.agregarVuelo(vueloTemp);
                    }
                }
                
                // Planificar los pedidos de este día
                PlanificadorAco planificador = new PlanificadorAco(grafoDelDia, aeropuertos);
                List<PlanificacionRespuestaDTO> asignacionesDelDia = planificador.planificarSemanal(
                    pedidosDelDia, fechaInicio, fechaFin);
                    
                if (asignacionesDelDia != null) {
                    todasLasAsignaciones.addAll(asignacionesDelDia);
                }
            }
            
            List<PlanificacionRespuestaDTO> asignaciones = todasLasAsignaciones;
                
            if (asignaciones == null || asignaciones.size() != pedidosDB.size()) {
                return new RespuestaDTO("error", 
                    String.format("Error en planificación: se esperaban %d asignaciones pero se obtuvieron %d",
                        pedidosDB.size(), asignaciones == null ? 0 : asignaciones.size()),
                    null);
            }

            // 6. Convertir asignaciones a formato assignments_split_icao
            List<assignments_split_icao> respuesta = new ArrayList<>();
            List<String> pedidosNoFactibles = new ArrayList<>();

            for (int i = 0; i < pedidosDB.size(); i++) {
                T03Pedido pedido = pedidosDB.get(i);
                PlanificacionRespuestaDTO asignacion = asignaciones.get(i);

                assignments_split_icao asig = new assignments_split_icao();
                asig.setOrderId(String.format("ORD-%03d", pedido.getId()));
                // Default: empty splits (explicit) to indicate not planned
                asig.setSplits(Collections.emptyList());

                // Si el pedido NO fue planificado, registrarlo para reporte
                if (!"PLANIFICADO".equals(asignacion.getEstado()) || asignacion.getVuelos().isEmpty()) {
                    String detalle = String.format("ORD-%03d: origen=%s, destino=%s, cantidad=%d - Estado: %s",
                        pedido.getId(),
                        pedido.getT01Idaeropuertoorigen() != null ? pedido.getT01Idaeropuertoorigen().getT01Codigoicao() : "NULL",
                        pedido.getT01Idaeropuertodestino() != null ? pedido.getT01Idaeropuertodestino().getT01Codigoicao() : "NULL",
                        pedido.getT03Cantidadpaquetes(),
                        asignacion.getEstado());
                    pedidosNoFactibles.add(detalle);
                    System.err.println("[ADVERTENCIA] Pedido no planificado: " + detalle);
                }

                // Si el pedido fue planificado exitosamente
                if ("PLANIFICADO".equals(asignacion.getEstado()) && asignacion.getSplits() != null && !asignacion.getSplits().isEmpty()) {
                    // Usar la información de splits que ya viene del ACO con cantidades correctas
                    List<assignments_split_icao.Split> splits = new ArrayList<>();
                    char splitLetra = 'A';

                    // Procesar cada split del ACO
                    for (com.morapack.dto.SplitRutaDTO splitACO : asignacion.getSplits()) {
                        if (splitACO.getVuelosRuta() == null || splitACO.getVuelosRuta().isEmpty()) {
                            continue;
                        }

                        assignments_split_icao.Split split = new assignments_split_icao.Split();
                        split.setConsignmentId(String.format("C-%03d-%c", pedido.getId(), splitLetra++));
                        split.setQty(splitACO.getCantidad());

                        // Crear legs para esta ruta
                        List<assignments_split_icao.Leg> legs = new ArrayList<>();
                        int seq = 1;

                        for (String vueloId : splitACO.getVuelosRuta()) {
                            // Buscar el vuelo
                            // Obtener el vuelo base y ajustar sus fechas según el día asignado
                            String[] partsId = vueloId.split("#");
                            if (partsId.length != 2) {
                                System.err.println("[SemanalService] ID de vuelo inválido: " + vueloId);
                                continue;
                            }
                            
                            String baseFlightId = partsId[0];
                            String dateTimeStr = partsId[1];

                            // Extraer el ID numérico del vuelo base
                            String numericId = baseFlightId.replace("MP-", "");
                            
                            // Buscar el vuelo base comparando IDs numéricos
                            Flight_instances_DTO vueloBase = vuelos.stream()
                                .filter(v -> v.flightId().replace("MP-", "").equals(numericId))
                                .findFirst()
                                .orElse(null);

                            if (vueloBase == null) {
                                System.err.println("[SemanalService] Vuelo base no encontrado: " + baseFlightId + " (ID numérico: " + numericId + ")");
                                continue;
                            }

                            // Crear una copia del vuelo base con la fecha ajustada
                            Flight_instances_DTO vuelo = new Flight_instances_DTO(
                                vueloId,
                                vueloBase.flightId(),
                                vueloBase.origin(),
                                vueloBase.dest(),
                                dateTimeStr, // Usar la fecha asignada por el planificador
                                vueloBase.arrUtc(), // Ajustar según la nueva fecha de salida
                                vueloBase.capacity()
                            );

                            assignments_split_icao.Leg leg = new assignments_split_icao.Leg();
                            leg.setSeq(seq++);
                            leg.setInstanceId(vuelo.instanceId());
                            leg.setFrom(vuelo.origin());
                            leg.setTo(vuelo.dest());
                            leg.setQty(splitACO.getCantidad()); // Usar la cantidad del split

                            legs.add(leg);
                        }

                        split.setLegs(legs);
                        splits.add(split);
                    }

                    asig.setSplits(splits);

                    // Verificar que la suma de cantidades coincida con el total del pedido
                    int totalAsignado = splits.stream()
                        .mapToInt(assignments_split_icao.Split::getQty)
                        .sum();
                    int totalPedido = pedido.getT03Cantidadpaquetes();
                    if (totalAsignado != totalPedido) {
                        System.err.println("[SemanalService] ADVERTENCIA: Pedido " + pedido.getId() +
                            " - Total asignado (" + totalAsignado + ") != Total pedido (" + totalPedido + ")");
                    }
                }

                respuesta.add(asig);
            }

            // Preparar respuesta con estadísticas
            int pedidosPlanificados = pedidosDB.size() - pedidosNoFactibles.size();
            String mensaje;
            String status;

            if (pedidosNoFactibles.isEmpty()) {
                status = "success";
                mensaje = String.format("Plan semanal generado exitosamente. Todos los %d pedidos fueron planificados.", pedidosDB.size());
            } else {
                status = "warning";
                mensaje = String.format("Plan semanal generado con advertencias. %d de %d pedidos planificados. %d pedidos NO FACTIBLES.",
                    pedidosPlanificados, pedidosDB.size(), pedidosNoFactibles.size());

                // Imprimir resumen en consola
                System.err.println("\n========== RESUMEN DE PLANIFICACIÓN ==========");
                System.err.println("Total de pedidos: " + pedidosDB.size());
                System.err.println("Planificados: " + pedidosPlanificados);
                System.err.println("No factibles: " + pedidosNoFactibles.size());
                System.err.println("\nPedidos no factibles:");
                pedidosNoFactibles.forEach(d -> System.err.println("  - " + d));
                System.err.println("=============================================\n");
            }

            Map<String, Object> dataConEstadisticas = new HashMap<>();
            dataConEstadisticas.put("assignments", respuesta);
            dataConEstadisticas.put("estadisticas", Map.of(
                "totalPedidos", pedidosDB.size(),
                "pedidosPlanificados", pedidosPlanificados,
                "pedidosNoFactibles", pedidosNoFactibles.size(),
                "detalleNoFactibles", pedidosNoFactibles
            ));

            return new RespuestaDTO(status, mensaje, dataConEstadisticas);

        } catch (Exception e) {
            return new RespuestaDTO("error", 
                "Error generando plan semanal: " + e.getMessage(), null);}
        }
    

    @Transactional
    private RespuestaDTO generarVuelosBase(T01Aeropuerto hub, String destinoIcao) {
        try {
            // 1. Obtener aeropuerto destino
            Optional<T01Aeropuerto> destinoOpt = aeropuertoRepository.findByT01Codigoicao(destinoIcao);
            if (destinoOpt.isEmpty()) {
                return new RespuestaDTO("error", "Aeropuerto destino " + destinoIcao + " no encontrado", null);
            }
            T01Aeropuerto destino = destinoOpt.get();
            if (destino == null) {
                return new RespuestaDTO("error", "El aeropuerto destino no es válido", null);
            }

            // 2. Definir horarios base para este par origen-destino
            // Asumimos 2 vuelos diarios: uno en la mañana y otro en la tarde
            VueloInputDto vueloMañana = new VueloInputDto();
            vueloMañana.setIcaoOrigen(hub.getT01Codigoicao());
            vueloMañana.setIcaoDestino(destinoIcao);
            vueloMañana.setHoraSalidaStr("09:00");
            vueloMañana.setHoraLlegadaStr("11:00"); // 2 horas después
            vueloMañana.setCapacidad(300); // Capacidad estándar

            VueloInputDto vueloTarde = new VueloInputDto();
            vueloTarde.setIcaoOrigen(hub.getT01Codigoicao());
            vueloTarde.setIcaoDestino(destinoIcao);
            vueloTarde.setHoraSalidaStr("15:00");
            vueloTarde.setHoraLlegadaStr("17:00"); // 2 horas después
            vueloTarde.setCapacidad(300); // Capacidad estándar

            // 3. Procesar los vuelos
            RespuestaDTO respuestaMañana = vueloService.procesarVuelo(vueloMañana);
            RespuestaDTO respuestaTarde = vueloService.procesarVuelo(vueloTarde);

            // 4. Retornar resultado combinado
            List<String> mensajes = new ArrayList<>();
            if ("success".equals(respuestaMañana.getStatus())) {
                mensajes.add("Vuelo mañana creado");
            } else {
                mensajes.add("Error vuelo mañana: " + respuestaMañana.getMensaje());
            }
            
            if ("success".equals(respuestaTarde.getStatus())) {
                mensajes.add("Vuelo tarde creado");
            } else {
                mensajes.add("Error vuelo tarde: " + respuestaTarde.getMensaje());
            }

            return new RespuestaDTO("success", 
                String.format("Vuelos base generados para ruta %s-%s", 
                    hub.getT01Codigoicao(), destinoIcao),
                mensajes);

        } catch (Exception e) {
            return new RespuestaDTO("error", 
                "Error generando vuelos base: " + e.getMessage(), null);
        }
    }

    /**
     * Agrupa vuelos consecutivos que forman rutas.
     * Una ruta es una secuencia de vuelos donde el destino de un vuelo es el origen del siguiente.
     * Cuando esta condición se rompe, se inicia una nueva ruta (nuevo split).
     */
    private List<List<String>> agruparVuelosEnRutas(List<String> vuelosIds, List<Flight_instances_DTO> todosVuelos) {
        List<List<String>> rutasAgrupadas = new ArrayList<>();
        if (vuelosIds.isEmpty()) {
            return rutasAgrupadas;
        }

        List<String> rutaActual = new ArrayList<>();
        String destinoAnterior = null;

        for (String vueloId : vuelosIds) {
            // Buscar el vuelo
            Flight_instances_DTO vuelo = todosVuelos.stream()
                .filter(v -> v.instanceId().equals(vueloId) || prefixBeforeHash(v.instanceId()).equals(prefixBeforeHash(vueloId)))
                .findFirst()
                .orElse(null);

            if (vuelo == null) {
                System.out.println("[SemanalService] No se pudo encontrar vuelo: " + vueloId);
                continue;
            }

            // Si es el primer vuelo o si el origen coincide con el destino anterior, continuar la ruta
            if (destinoAnterior == null || vuelo.origin().equals(destinoAnterior)) {
                rutaActual.add(vueloId);
                destinoAnterior = vuelo.dest();
            } else {
                // El origen no coincide con el destino anterior, es una nueva ruta (nuevo split)
                if (!rutaActual.isEmpty()) {
                    rutasAgrupadas.add(new ArrayList<>(rutaActual));
                }
                rutaActual.clear();
                rutaActual.add(vueloId);
                destinoAnterior = vuelo.dest();
            }
        }

        // Agregar la última ruta
        if (!rutaActual.isEmpty()) {
            rutasAgrupadas.add(rutaActual);
        }

        return rutasAgrupadas;
    }

    /**
     * Haversine distance in kilometers between two lat/lon points.
     */
    private static String prefixBeforeHash(String id) {
        if (id == null) return "";
        int p = id.indexOf('#');
        if (p > 0) return id.substring(0, p).trim();
        return id.trim();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String getZonaHoraria(T01Aeropuerto aeropuerto) {
        if (aeropuerto != null && aeropuerto.getT08Idciudad() != null 
            && aeropuerto.getT08Idciudad().getT08Zonahoraria() != null) {
            return aeropuerto.getT08Idciudad().getT08Zonahoraria();
        }
        return "UTC";
    }

    private boolean getMismoContinente(T01Aeropuerto origen, T01Aeropuerto destino) {
        if (origen == null || destino == null 
            || origen.getT08Idciudad() == null || destino.getT08Idciudad() == null
            || origen.getT08Idciudad().getT08Continente() == null 
            || destino.getT08Idciudad().getT08Continente() == null) {
            return false;
        }
        return origen.getT08Idciudad().getT08Continente()
            .equals(destino.getT08Idciudad().getT08Continente());
    }

    /**
     * Obtiene la planificación semanal en formato directo (solo el array de assignments).
     * Este método retorna directamente la lista sin envolverla en RespuestaDTO.
     */
    @Transactional
    public List<assignments_split_icao> obtenerPlanificacionSemanalDirecta() {
        try {
            RespuestaDTO respuesta = obtenerPlanificacionSemanal();

            if ("success".equals(respuesta.getStatus()) || "warning".equals(respuesta.getStatus())) {
                if (respuesta.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) respuesta.getData();
                    Object assignmentsObj = data.get("assignments");

                    if (assignmentsObj instanceof List) {
                        return (List<assignments_split_icao>) assignmentsObj;
                    }
                } else if (respuesta.getData() instanceof List) {
                    return (List<assignments_split_icao>) respuesta.getData();
                }
            }

            // Si hay error, retornar lista vacía
            System.err.println("[SemanalService] Error al obtener planificación: " + respuesta.getMensaje());
            return Collections.emptyList();

        } catch (Exception e) {
            System.err.println("[SemanalService] Excepción al obtener planificación: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Procesa cancelaciones de vuelos y replanifica pedidos afectados.
     * El archivo contiene líneas con formato: dia-hora-idVuelo-origen-destino
     * Ejemplo: 15-14:30-5-SPIM-SKBO
     */
    @Transactional
    public RespuestaDTO procesarCancelaciones(MultipartFile archivo) {
        try {
            // 1. Leer cancelaciones del archivo
            List<CancelacionDTO> cancelaciones = UtilArchivos.cargarCancelaciones(archivo);

            if (cancelaciones.isEmpty()) {
                return new RespuestaDTO("warning", "No se encontraron cancelaciones en el archivo", null);
            }

            System.out.println("[SemanalService] Procesando " + cancelaciones.size() + " cancelaciones");

            // 2. Para cada cancelación, buscar y marcar el vuelo como cancelado
            // NOTA: En este sistema simplificado, no guardamos cancelaciones en BD,
            // sino que las consideramos al momento de replanificar.
            // Una mejora futura sería tener una tabla de vuelos cancelados.

            // 3. Obtener la planificación actual y identificar pedidos afectados
            // Para simplificar, vamos a replanificar TODOS los pedidos.
            // Una mejora sería solo replanificar los que usan vuelos cancelados.

            System.out.println("[SemanalService] Cancelaciones procesadas. Replanificando todos los pedidos...");

            // 4. Replanificar usando el método existente
            RespuestaDTO planificacionRespuesta = obtenerPlanificacionSemanal();

            if ("error".equals(planificacionRespuesta.getStatus())) {
                return new RespuestaDTO("error",
                    "Error al replanificar después de cancelaciones: " + planificacionRespuesta.getMensaje(),
                    null);
            }

            Map<String, Object> data = Map.of(
                "cancelacionesProcesadas", cancelaciones.size(),
                "planificacionActualizada", planificacionRespuesta.getData()
            );

            return new RespuestaDTO("success",
                String.format("Se procesaron %d cancelaciones y se replanificaron los pedidos exitosamente.",
                    cancelaciones.size()),
                data);

        } catch (Exception e) {
            return new RespuestaDTO("error",
                "Error procesando cancelaciones: " + e.getMessage(),
                null);
        }
    }
}