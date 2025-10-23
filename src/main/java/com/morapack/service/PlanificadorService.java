package com.morapack.service;

import com.morapack.dto.PlanResponse;
import com.morapack.util.PlanConverter;
import com.morapack.model.T01Aeropuerto;
import com.morapack.model.T03Pedido;
import com.morapack.model.T06VueloProgramado;
import com.morapack.nucleo.AppPlanificador;
import com.morapack.nucleo.GrafoVuelos;
import com.morapack.simulador.CancellationRecord;
import com.morapack.service.EstadosTemporales;
import com.morapack.planificador.util.UtilArchivos;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PlanificadorService {

    public List<PlanResponse> generateWeeklyPlan(Path aeropTxt, Path vuelosTxt, Path pedidosTxt,
                                                 Path cancelTxt, YearMonth periodo) throws IOException {
        // Ventana semanal (ajustable según necesidad)
        Instant tStart = ZonedDateTime.of(periodo.getYear(), periodo.getMonthValue(), 1,
                10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant tEnd = ZonedDateTime.of(periodo.getYear(), periodo.getMonthValue(), 7,
                23, 59, 59, 0, ZoneOffset.UTC).toInstant();

        // Carga de datos
        Map<String, Aeropuerto> aeropuertos = UtilArchivos.leerAeropuertos(aeropTxt);
        List<Vuelo> vuelos = UtilArchivos.leerVuelos(vuelosTxt);
        List<Pedido> pedidos = UtilArchivos.leerPedidos(pedidosTxt, periodo);
        List<CancellationRecord> cancels = java.nio.file.Files.exists(cancelTxt)
                ? UtilArchivos.leerCancelaciones(cancelTxt, aeropuertos, periodo)
                : Collections.emptyList();

        // Backend autoritativo
        GrafoVuelos grafo = new GrafoVuelos(vuelos);
        AppPlanificador app = new AppPlanificador(aeropuertos, grafo);

        // Compilar plan semanal
        String version = app.compileWeekly(tStart, tEnd, pedidos, periodo);

        // Aplicar cancelaciones si existen
        if (!cancels.isEmpty()) {
            var sum = app.applyCancelations(cancels);
            // Replanificar inmediatamente si hay cancelaciones
            app.replanPending(tStart.plus(java.time.Duration.ofHours(12)), periodo);
        }

        // Obtener el estado final del plan
        EstadosTemporales estadosFinales = app.stateAt(tEnd);

        // Convertir el plan al formato de respuesta requerido
        return PlanConverter.convertToPlanResponse(estadosFinales);
    }
}