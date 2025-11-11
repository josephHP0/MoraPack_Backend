-- =====================================================
-- Crear tablas faltantes de simulación
-- Usando nomenclatura camelCase existente
-- =====================================================

USE morapack2;

-- =====================================================
-- T13_METRICA_DIARIA
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
    FOREIGN KEY (T01_idAeropuerto) REFERENCES t01_aeropuerto(T01_ID_AEROPUERTO)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_alert_sim (T10_idSimulacion),
    INDEX idx_alert_aerop (T01_idAeropuerto),
    INDEX idx_alert_fecha (T14_fechaHora),
    INDEX idx_alert_severidad (T14_severidad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alertas de congestión en aeropuertos';

-- =====================================================
-- T15_TOP_AEROPUERTO_WPS
-- =====================================================

CREATE TABLE IF NOT EXISTS t15_top_aeropuerto_wps (
    T15_idTop INT AUTO_INCREMENT PRIMARY KEY,
    T13_idMetrica INT NOT NULL COMMENT 'Métrica diaria asociada',
    T01_idAeropuerto INT NOT NULL COMMENT 'Aeropuerto rankeado',
    T15_wpsMaximo DECIMAL(5,2) NOT NULL COMMENT 'WPS máximo alcanzado en el día',
    T15_ranking INT NOT NULL COMMENT 'Posición en el ranking (1-5)',

    FOREIGN KEY (T13_idMetrica) REFERENCES t13_metrica_diaria(T13_idMetrica)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (T01_idAeropuerto) REFERENCES t01_aeropuerto(T01_ID_AEROPUERTO)
        ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_top_metrica (T13_idMetrica),
    INDEX idx_top_ranking (T15_ranking)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Top-5 aeropuertos por WPS diario';

-- =====================================================
-- Verificación
-- =====================================================

SELECT
    'Tablas completadas exitosamente' AS status,
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
