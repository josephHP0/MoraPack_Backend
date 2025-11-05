package com.morapack.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class PlanificacionSemanalDTO {
    private List<RutaPedidoDTO> rutas;
    private MetricasDTO metricas;

    @Data
    public static class RutaPedidoDTO {
        private Integer idPedido;
        private Integer cantidadPaquetes;
        private List<TramoDTO> tramos;
        private boolean cumplePlazo;
    }

    @Data
    public static class TramoDTO {
        private Integer idVuelo;
        private String aeropuertoOrigen;
        private String aeropuertoDestino;
        private Instant fechaSalida;
        private Instant fechaLlegada;
        private Integer cantidadPaquetes;
    }

    @Data
    public static class MetricasDTO {
        private double tasaCumplimiento;
        private int totalPedidos;
        private int pedidosPlanificados;
        private int totalPaquetes;
        private int paquetesPlanificados;
        private double utilizacionPromedio;
        private double tiempoPromedioEntrega;
    }
}