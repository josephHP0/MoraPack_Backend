package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.common.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CalculadorSLA {

    private final CiudadRepository ciudadRepository;
    private static final long HORAS_PROCESAMIENTO_DESTINO = 2;

    /**
     * Calcula el SLA en horas según las reglas:
     * - Mismo continente: 2 días (48 horas)
     * - Continentes diferentes: 3 días (72 horas)
     */
    public long calcularSlaHoras(T02Pedido pedido) {
        // TODO: Implementar lógica real que verifique continente de productos vs destino
        // Por ahora, asumimos continente diferente (3 días)
        return 72;
    }

    /**
     * Calcula la fecha límite de entrega considerando:
     * - Fecha/hora del pedido en uso horario del destino
     * - SLA según continente
     * - 2 horas adicionales de procesamiento en destino
     */
    public Instant calcularFechaLimite(T02Pedido pedido) {
        long slaHoras = calcularSlaHoras(pedido);
        return pedido.getT02FechaPedido().plusSeconds((slaHoras + HORAS_PROCESAMIENTO_DESTINO) * 3600);
    }

    /**
     * Verifica si una entrega estimada cumple con el SLA
     */
    public boolean cumpleSLA(T02Pedido pedido, Instant fechaEntregaEstimada) {
        Instant fechaLimite = calcularFechaLimite(pedido);
        return fechaEntregaEstimada.isBefore(fechaLimite) || fechaEntregaEstimada.equals(fechaLimite);
    }

    /**
     * Calcula tiempo restante hasta vencimiento del SLA
     */
    public long calcularTiempoRestanteHoras(T02Pedido pedido) {
        Instant ahora = Instant.now();
        Instant fechaLimite = calcularFechaLimite(pedido);
        return Duration.between(ahora, fechaLimite).toHours();
    }
}
