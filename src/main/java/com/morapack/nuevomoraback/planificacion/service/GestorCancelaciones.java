package com.morapack.nuevomoraback.planificacion.service;

import com.morapack.nuevomoraback.common.domain.T04VueloProgramado;
import com.morapack.nuevomoraback.common.repository.VueloProgramadoRepository;
import com.morapack.nuevomoraback.planificacion.domain.T12CancelacionVuelo;
import com.morapack.nuevomoraback.planificacion.repository.T12CancelacionVueloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestorCancelaciones {

    private final T12CancelacionVueloRepository cancelacionRepository;
    private final VueloProgramadoRepository vueloRepository;

    /**
     * Aplica cancelaciones programadas en un rango de tiempo
     * Solo cancela vuelos que están en tierra (antes del despegue)
     *
     * NOTA: Si la tabla T12_CANCELACION_VUELO está vacía, este método
     * retornará 0 y la simulación seguirá su flujo normal sin cancelaciones.
     */
    @Transactional
    public int aplicarCancelaciones(Instant fechaInicio, Instant fechaFin) {
        List<T12CancelacionVuelo> cancelaciones =
            cancelacionRepository.findCancelacionesEnRango(fechaInicio, fechaFin);

        if (cancelaciones.isEmpty()) {
            log.info("No hay cancelaciones programadas en el periodo {} a {}", fechaInicio, fechaFin);
            return 0;
        }

        log.info("Aplicando {} cancelaciones programadas", cancelaciones.size());
        int cancelados = 0;

        for (T12CancelacionVuelo cancelacion : cancelaciones) {
            T04VueloProgramado vuelo = cancelacion.getVueloProgramado();

            // Solo cancelar si aún no ha despegado
            if (cancelacion.getFechaCancelacion().isBefore(vuelo.getT04FechaSalida())) {
                vuelo.setT04Estado("CANCELADO");
                vueloRepository.save(vuelo);
                cancelados++;

                log.info("Vuelo {} cancelado: {}", vuelo.getId(), cancelacion.getMotivo());
            } else {
                log.warn("No se puede cancelar vuelo {} - Ya despegó o está en vuelo", vuelo.getId());
            }
        }

        log.info("Total de vuelos cancelados: {}", cancelados);
        return cancelados;
    }

    /**
     * Verifica si un vuelo específico está cancelado
     */
    public boolean estaCancelado(Integer vueloId) {
        T04VueloProgramado vuelo = vueloRepository.findById(vueloId).orElse(null);
        return vuelo != null && "CANCELADO".equals(vuelo.getT04Estado());
    }
}
