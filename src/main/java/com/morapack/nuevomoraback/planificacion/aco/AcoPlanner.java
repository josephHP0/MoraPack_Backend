package com.morapack.nuevomoraback.planificacion.aco;

import com.morapack.nuevomoraback.common.domain.T02Pedido;
import com.morapack.nuevomoraback.planificacion.domain.T08RutaPlaneada;

import java.util.List;

public interface AcoPlanner {

    /**
     * Ejecuta el algoritmo ACO para planificar pedidos
     * @param pedidos Lista de pedidos a planificar
     * @param tipoSimulacion Tipo de simulación (SEMANAL, DIA_A_DIA, COLAPSO)
     * @return Lista de rutas planeadas
     */
    List<T08RutaPlaneada> planificar(List<T02Pedido> pedidos,
                                      T08RutaPlaneada.TipoSimulacion tipoSimulacion);
}
