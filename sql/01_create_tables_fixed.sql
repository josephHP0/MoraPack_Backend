-- =====================================================
-- Script de Creación de Tablas para Simulación Semanal
-- MoraPack Backend - Versión 1.0 (Corregido)
-- =====================================================

USE morapack2;

-- =====================================================
-- T10_SIMULACION_SEMANAL
-- Registro de simulaciones semanales de planificación
-- =====================================================

CREATE TABLE IF NOT EXISTS t10_simulacion_semanal (
    T10_idSimulacion INT AUTO_INCREMENT PRIMARY KEY,
    T10_fechaInicio DATETIME NOT NULL COMMENT 'Inicio de la ventana de simulación (UTC)',
    T10_fechaFin DATETIME NOT NULL COMMENT 'Fin de la ventana de simulación (UTC)',
    T10_fechaCreacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
    T10_estado VARCHAR(30) DEFAULT 'EN_PROGRESO' COMMENT 'Estado: EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA',
    T10_pedidosProcesados INT DEFAULT 0 COMMENT 'Número total de pedidos procesados',
    T10_pedidosAsignados INT DEFAULT 0 COMMENT 'Número de pedidos con ruta asignada exitosamente',
    T10_pedidosPendientes INT DEFAULT 0 COMMENT 'Número de pedidos sin ruta factible',
    T10_motivoFallo VARCHAR(500) NULL COMMENT 'Descripción del error si estado=FALLIDA',
    T10_duracionMs BIGINT NULL COMMENT 'Duración de la simulación en milisegundos',

    INDEX idx_sim_estado (T10_estado),
    INDEX idx_sim_fecha (T10_fechaCreacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Registro de simulaciones semanales';

-- =====================================================
-- T12_CANCELACION
-- Cancelaciones de vuelos (manuales, por archivo o automáticas)
-- =====================================================

CREATE TABLE IF NOT EXISTS t12_cancelacion (
    T12_idCancelacion INT AUTO_INCREMENT PRIMARY KEY,
    T04_idTramoVuelo INT NOT NULL COMMENT 'Vuelo cancelado',
    T12_fechaCancelacion DATETIME NOT NULL COMMENT 'Fecha y hora de la cancelación',
    T12_motivo VARCHAR(200) NULL COMMENT 'Motivo de la cancelación',
    T12_origen VARCHAR(20) DEFAULT 'MANUAL' COMMENT 'Origen: MANUAL, ARCHIVO, SISTEMA',

    FOREIGN KEY (T04_idTramoVuelo) REFERENCES t04_vuelo_programado(T04_idTramoVuelo)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_canc_vuelo (T04_idTramoVuelo),
    INDEX idx_canc_fecha (T12_fechaCancelacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cancelaciones de vuelos';

-- =====================================================
-- T13_METRICA_DIARIA
-- Métricas agregadas por día de simulación
-- =====================================================

CREATE TABLE IF NOT EXISTS t13_metrica_diaria (
    T13_idMetrica INT AUTO_INCREMENT PRIMARY KEY,
    T10_idSimulacion INT NOT NULL COMMENT 'Simulación asociada',
    T13_fecha DATE NOT NULL COMMENT 'Fecha de la métrica',
    T13_capacidadMediaAlmacenes DECIMAL(5,2) NULL COMMENT 'Capacidad media de almacenes (%)',
    T13_wpsPico95 DECIMAL(5,2) NULL COMMENT 'Percentil 95 de WPS',
    T13_wpsPico99 DECIMAL(5,2) NULL COMMENT 'Percentil 99 de WPS',
    T13_rutasDescartadasHeadroom INT DEFAULT 0 COMMENT 'Rutas descartadas por headroom insuficiente',
    T13_rutasDescartadasCapacidad INT DEFAULT 0 COMMENT 'Rutas descartadas por capacidad insuficiente',
    T13_pedidosEntregados INT DEFAULT 0 COMMENT 'Pedidos completados en el día',
    T13_pedidosPendientes INT DEFAULT 0 COMMENT 'Pedidos pendientes al final del día',
    T13_tiempoMedioEntregaHoras DECIMAL(6,2) NULL COMMENT 'Tiempo medio de entrega en horas',

    FOREIGN KEY (T10_idSimulacion) REFERENCES t10_simulacion_semanal(T10_idSimulacion)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_met_sim (T10_idSimulacion),
    INDEX idx_met_fecha (T13_fecha),
    UNIQUE KEY uk_met_sim_fecha (T10_idSimulacion, T13_fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Métricas diarias de simulación';

-- =====================================================
-- T14_ALERTA_NEAR_COLLAPSE
-- Alertas de congestión (near-collapse) en aeropuertos
-- =====================================================

CREATE TABLE IF NOT EXISTS t14_alerta_near_collapse (
    T14_idAlerta INT AUTO_INCREMENT PRIMARY KEY,
    T10_idSimulacion INT NOT NULL COMMENT 'Simulación asociada',
    T01_idAeropuerto INT NOT NULL COMMENT 'Aeropuerto en alerta',
    T14_fechaHora DATETIME NOT NULL COMMENT 'Momento de la alerta',
    T14_wps DECIMAL(5,2) NOT NULL COMMENT 'Warehouse Pressure Score',
    T14_ocupacion INT NOT NULL COMMENT 'Ocupación actual (paquetes)',
    T14_capacidad INT NOT NULL COMMENT 'Capacidad del aeropuerto',
    T14_severidad VARCHAR(20) DEFAULT 'MEDIA' COMMENT 'Severidad: BAJA, MEDIA, ALTA, CRITICA',
    T14_resuelto BOOLEAN DEFAULT FALSE COMMENT 'Si la alerta fue resuelta',

    FOREIGN KEY (T10_idSimulacion) REFERENCES t10_simulacion_semanal(T10_idSimulacion)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (T01_idAeropuerto) REFERENCES t01_aeropuerto(T01_idAeropuerto)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_alert_sim (T10_idSimulacion),
    INDEX idx_alert_aerop (T01_idAeropuerto),
    INDEX idx_alert_fecha (T14_fechaHora),
    INDEX idx_alert_severidad (T14_severidad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alertas de congestión en aeropuertos';

-- =====================================================
-- T15_TOP_AEROPUERTO_WPS
-- Ranking top-5 de aeropuertos por WPS (para métricas diarias)
-- =====================================================

CREATE TABLE IF NOT EXISTS t15_top_aeropuerto_wps (
    T15_idTop INT AUTO_INCREMENT PRIMARY KEY,
    T13_idMetrica INT NOT NULL COMMENT 'Métrica diaria asociada',
    T01_idAeropuerto INT NOT NULL COMMENT 'Aeropuerto rankeado',
    T15_wpsMaximo DECIMAL(5,2) NOT NULL COMMENT 'WPS máximo alcanzado en el día',
    T15_ranking INT NOT NULL COMMENT 'Posición en el ranking (1-5)',

    FOREIGN KEY (T13_idMetrica) REFERENCES t13_metrica_diaria(T13_idMetrica)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (T01_idAeropuerto) REFERENCES t01_aeropuerto(T01_idAeropuerto)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_top_metrica (T13_idMetrica),
    INDEX idx_top_ranking (T15_ranking)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Top-5 aeropuertos por WPS diario';

-- =====================================================
-- Verificación
-- =====================================================

SELECT
    'Tablas creadas exitosamente' AS status,
    COUNT(*) AS total_tables
FROM information_schema.tables
WHERE table_schema = 'morapack2'
  AND table_name IN (
      't10_simulacion_semanal',
      't12_cancelacion',
      't13_metrica_diaria',
      't14_alerta_near_collapse',
      't15_top_aeropuerto_wps'
  );
