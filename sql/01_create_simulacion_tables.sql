-- =====================================================
-- Script de Creación de Tablas para Simulación Semanal
-- MoraPack Backend - Versión 1.0 (Ajustado a nomenclatura BD)
-- =====================================================

USE morapack2;

-- =====================================================
-- T10_SIMULACION_SEMANAL
-- Registro de simulaciones semanales de planificación
-- =====================================================

CREATE TABLE IF NOT EXISTS t10_simulacion_semanal (
    T10_ID_SIMULACION INT AUTO_INCREMENT PRIMARY KEY,
    T10_FECHA_INICIO DATETIME NOT NULL COMMENT 'Inicio de la ventana de simulación (UTC)',
    T10_FECHA_FIN DATETIME NOT NULL COMMENT 'Fin de la ventana de simulación (UTC)',
    T10_FECHA_CREACION TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
    T10_ESTADO VARCHAR(30) DEFAULT 'EN_PROGRESO' COMMENT 'Estado: EN_PROGRESO, COMPLETADA, FALLIDA, CANCELADA',
    T10_PEDIDOS_PROCESADOS INT DEFAULT 0 COMMENT 'Número total de pedidos procesados',
    T10_PEDIDOS_ASIGNADOS INT DEFAULT 0 COMMENT 'Número de pedidos con ruta asignada exitosamente',
    T10_PEDIDOS_PENDIENTES INT DEFAULT 0 COMMENT 'Número de pedidos sin ruta factible',
    T10_MOTIVO_FALLO VARCHAR(500) NULL COMMENT 'Descripción del error si estado=FALLIDA',
    T10_DURACION_MS BIGINT NULL COMMENT 'Duración de la simulación en milisegundos',

    INDEX idx_sim_estado (T10_ESTADO),
    INDEX idx_sim_fecha (T10_FECHA_CREACION)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Registro de simulaciones semanales';

-- =====================================================
-- T12_CANCELACION
-- Cancelaciones de vuelos (manuales, por archivo o automáticas)
-- =====================================================

CREATE TABLE IF NOT EXISTS t12_cancelacion (
    T12_ID_CANCELACION INT AUTO_INCREMENT PRIMARY KEY,
    T04_ID_TRAMO_VUELO INT NOT NULL COMMENT 'Vuelo cancelado',
    T12_FECHA_CANCELACION DATETIME NOT NULL COMMENT 'Fecha y hora de la cancelación',
    T12_MOTIVO VARCHAR(200) NULL COMMENT 'Motivo de la cancelación',
    T12_ORIGEN VARCHAR(20) DEFAULT 'MANUAL' COMMENT 'Origen: MANUAL, ARCHIVO, SISTEMA',

    FOREIGN KEY (T04_ID_TRAMO_VUELO) REFERENCES t04_vuelo_programado(T04_ID_TRAMO_VUELO)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_canc_vuelo (T04_ID_TRAMO_VUELO),
    INDEX idx_canc_fecha (T12_FECHA_CANCELACION)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Cancelaciones de vuelos';

-- =====================================================
-- T13_METRICA_DIARIA
-- Métricas agregadas por día de simulación
-- =====================================================

CREATE TABLE IF NOT EXISTS t13_metrica_diaria (
    T13_ID_METRICA INT AUTO_INCREMENT PRIMARY KEY,
    T10_ID_SIMULACION INT NOT NULL COMMENT 'Simulación asociada',
    T13_FECHA DATE NOT NULL COMMENT 'Fecha de la métrica',
    T13_CAPACIDAD_MEDIA_ALMACENES DECIMAL(5,2) NULL COMMENT 'Capacidad media de almacenes (%)',
    T13_WPS_PICO_95 DECIMAL(5,2) NULL COMMENT 'Percentil 95 de WPS',
    T13_WPS_PICO_99 DECIMAL(5,2) NULL COMMENT 'Percentil 99 de WPS',
    T13_RUTAS_DESCARTADAS_HEADROOM INT DEFAULT 0 COMMENT 'Rutas descartadas por headroom insuficiente',
    T13_RUTAS_DESCARTADAS_CAPACIDAD INT DEFAULT 0 COMMENT 'Rutas descartadas por capacidad insuficiente',
    T13_PEDIDOS_ENTREGADOS INT DEFAULT 0 COMMENT 'Pedidos completados en el día',
    T13_PEDIDOS_PENDIENTES INT DEFAULT 0 COMMENT 'Pedidos pendientes al final del día',
    T13_TIEMPO_MEDIO_ENTREGA_HORAS DECIMAL(6,2) NULL COMMENT 'Tiempo medio de entrega en horas',

    FOREIGN KEY (T10_ID_SIMULACION) REFERENCES t10_simulacion_semanal(T10_ID_SIMULACION)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_met_sim (T10_ID_SIMULACION),
    INDEX idx_met_fecha (T13_FECHA),
    UNIQUE KEY uk_met_sim_fecha (T10_ID_SIMULACION, T13_FECHA)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Métricas diarias de simulación';

-- =====================================================
-- T14_ALERTA_NEAR_COLLAPSE
-- Alertas de congestión (near-collapse) en aeropuertos
-- =====================================================

CREATE TABLE IF NOT EXISTS t14_alerta_near_collapse (
    T14_ID_ALERTA INT AUTO_INCREMENT PRIMARY KEY,
    T10_ID_SIMULACION INT NOT NULL COMMENT 'Simulación asociada',
    T01_ID_AEROPUERTO INT NOT NULL COMMENT 'Aeropuerto en alerta',
    T14_FECHA_HORA DATETIME NOT NULL COMMENT 'Momento de la alerta',
    T14_WPS DECIMAL(5,2) NOT NULL COMMENT 'Warehouse Pressure Score',
    T14_OCUPACION INT NOT NULL COMMENT 'Ocupación actual (paquetes)',
    T14_CAPACIDAD INT NOT NULL COMMENT 'Capacidad del aeropuerto',
    T14_SEVERIDAD VARCHAR(20) DEFAULT 'MEDIA' COMMENT 'Severidad: BAJA, MEDIA, ALTA, CRITICA',
    T14_RESUELTO BOOLEAN DEFAULT FALSE COMMENT 'Si la alerta fue resuelta',

    FOREIGN KEY (T10_ID_SIMULACION) REFERENCES t10_simulacion_semanal(T10_ID_SIMULACION)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (T01_ID_AEROPUERTO) REFERENCES t01_aeropuerto(T01_ID_AEROPUERTO)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_alert_sim (T10_ID_SIMULACION),
    INDEX idx_alert_aerop (T01_ID_AEROPUERTO),
    INDEX idx_alert_fecha (T14_FECHA_HORA),
    INDEX idx_alert_severidad (T14_SEVERIDAD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alertas de congestión en aeropuertos';

-- =====================================================
-- T15_TOP_AEROPUERTO_WPS
-- Ranking top-5 de aeropuertos por WPS (para métricas diarias)
-- =====================================================

CREATE TABLE IF NOT EXISTS t15_top_aeropuerto_wps (
    T15_ID_TOP INT AUTO_INCREMENT PRIMARY KEY,
    T13_ID_METRICA INT NOT NULL COMMENT 'Métrica diaria asociada',
    T01_ID_AEROPUERTO INT NOT NULL COMMENT 'Aeropuerto rankeado',
    T15_WPS_MAXIMO DECIMAL(5,2) NOT NULL COMMENT 'WPS máximo alcanzado en el día',
    T15_RANKING INT NOT NULL COMMENT 'Posición en el ranking (1-5)',

    FOREIGN KEY (T13_ID_METRICA) REFERENCES t13_metrica_diaria(T13_ID_METRICA)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (T01_ID_AEROPUERTO) REFERENCES t01_aeropuerto(T01_ID_AEROPUERTO)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_top_metrica (T13_ID_METRICA),
    INDEX idx_top_ranking (T15_RANKING)
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
