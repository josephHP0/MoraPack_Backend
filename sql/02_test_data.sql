-- =====================================================
-- Script de Datos de Prueba para Simulación Semanal
-- MoraPack Backend - Versión 1.0
-- =====================================================

USE morapack2;

-- =====================================================
-- Datos de Prueba: Simulación Semanal
-- =====================================================

-- Simulación completada exitosamente
INSERT INTO t10_simulacion_semanal (
    T10_fechaInicio,
    T10_fechaFin,
    T10_fechaCreacion,
    T10_estado,
    T10_pedidosProcesados,
    T10_pedidosAsignados,
    T10_pedidosPendientes,
    T10_duracionMs
) VALUES (
    '2025-11-01 00:00:00',
    '2025-11-07 23:59:59',
    CURRENT_TIMESTAMP,
    'COMPLETADA',
    1000,
    950,
    50,
    1234567
);

-- Simulación en progreso
INSERT INTO t10_simulacion_semanal (
    T10_fechaInicio,
    T10_fechaFin,
    T10_fechaCreacion,
    T10_estado,
    T10_pedidosProcesados,
    T10_pedidosAsignados,
    T10_pedidosPendientes
) VALUES (
    '2025-11-08 00:00:00',
    '2025-11-14 23:59:59',
    CURRENT_TIMESTAMP,
    'EN_PROGRESO',
    450,
    430,
    20
);

-- Simulación fallida
INSERT INTO t10_simulacion_semanal (
    T10_fechaInicio,
    T10_fechaFin,
    T10_fechaCreacion,
    T10_estado,
    T10_pedidosProcesados,
    T10_pedidosAsignados,
    T10_pedidosPendientes,
    T10_motivoFallo,
    T10_duracionMs
) VALUES (
    '2025-10-25 00:00:00',
    '2025-10-31 23:59:59',
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY),
    'FALLIDA',
    100,
    95,
    5,
    'Error de conexión a la base de datos durante procesamiento',
    45000
);

-- =====================================================
-- Datos de Prueba: Cancelaciones
-- =====================================================

-- Cancelación manual
INSERT INTO t12_cancelacion (
    T06_idTramoVuelo,
    T12_fechaCancelacion,
    T12_motivo,
    T12_origen
) VALUES (
    1,  -- Asume que existe T06_idTramoVuelo=1
    '2025-11-02 08:30:00',
    'Mantenimiento no programado',
    'MANUAL'
);

-- Cancelación por archivo
INSERT INTO t12_cancelacion (
    T06_idTramoVuelo,
    T12_fechaCancelacion,
    T12_motivo,
    T12_origen
) VALUES (
    2,  -- Asume que existe T06_idTramoVuelo=2
    '2025-11-02 15:00:00',
    'Cancelación masiva por clima adverso',
    'ARCHIVO'
);

-- =====================================================
-- Datos de Prueba: Métricas Diarias
-- =====================================================

-- Métricas para simulación completada (día 1)
INSERT INTO t13_metrica_diaria (
    T10_idSimulacion,
    T13_fecha,
    T13_capacidadMediaAlmacenes,
    T13_wpsPico95,
    T13_wpsPico99,
    T13_rutasDescartadasHeadroom,
    T13_rutasDescartadasCapacidad,
    T13_pedidosEntregados,
    T13_pedidosPendientes,
    T13_tiempoMedioEntregaHoras
) VALUES (
    1,  -- Primera simulación
    '2025-11-01',
    65.50,
    0.85,
    1.05,
    12,
    8,
    145,
    5,
    38.25
);

-- Métricas para día 2
INSERT INTO t13_metrica_diaria (
    T10_idSimulacion,
    T13_fecha,
    T13_capacidadMediaAlmacenes,
    T13_wpsPico95,
    T13_wpsPico99,
    T13_rutasDescartadasHeadroom,
    T13_rutasDescartadasCapacidad,
    T13_pedidosEntregados,
    T13_pedidosPendientes,
    T13_tiempoMedioEntregaHoras
) VALUES (
    1,
    '2025-11-02',
    72.30,
    0.92,
    1.18,
    18,
    15,
    130,
    20,
    42.10
);

-- =====================================================
-- Datos de Prueba: Alertas Near-Collapse
-- =====================================================

-- Alerta MEDIA
INSERT INTO t14_alerta_near_collapse (
    T10_idSimulacion,
    T01_idAeropuerto,
    T14_fechaHora,
    T14_wps,
    T14_ocupacion,
    T14_capacidad,
    T14_severidad,
    T14_resuelto
) VALUES (
    1,
    31,  -- Asume SKBO (Bogotá) tiene id=31
    '2025-11-02 14:00:00',
    0.75,
    320,
    430,
    'MEDIA',
    TRUE
);

-- Alerta ALTA
INSERT INTO t14_alerta_near_collapse (
    T10_idSimulacion,
    T01_idAeropuerto,
    T14_fechaHora,
    T14_wps,
    T14_ocupacion,
    T14_capacidad,
    T14_severidad,
    T14_resuelto
) VALUES (
    1,
    33,  -- Asume SEQM (Quito) tiene id=33
    '2025-11-02 18:30:00',
    0.95,
    385,
    410,
    'ALTA',
    TRUE
);

-- Alerta CRITICA no resuelta
INSERT INTO t14_alerta_near_collapse (
    T10_idSimulacion,
    T01_idAeropuerto,
    T14_fechaHora,
    T14_wps,
    T14_ocupacion,
    T14_capacidad,
    T14_severidad,
    T14_resuelto
) VALUES (
    1,
    36,  -- Asume SPIM (Lima) tiene id=36
    '2025-11-03 10:00:00',
    1.25,
    425,
    440,
    'CRITICA',
    FALSE
);

-- =====================================================
-- Datos de Prueba: Top-5 Aeropuertos por WPS
-- =====================================================

-- Top-5 para día 1
INSERT INTO t15_top_aeropuerto_wps (T13_idMetrica, T01_idAeropuerto, T15_wpsMaximo, T15_ranking)
VALUES
    (1, 36, 1.05, 1),  -- Lima
    (1, 33, 0.92, 2),  -- Quito
    (1, 31, 0.85, 3),  -- Bogotá
    (1, 45, 0.78, 4),  -- Bruselas
    (1, 60, 0.65, 5);  -- Baku

-- Top-5 para día 2
INSERT INTO t15_top_aeropuerto_wps (T13_idMetrica, T01_idAeropuerto, T15_wpsMaximo, T15_ranking)
VALUES
    (2, 36, 1.18, 1),  -- Lima
    (2, 45, 0.95, 2),  -- Bruselas
    (2, 33, 0.88, 3),  -- Quito
    (2, 60, 0.82, 4),  -- Baku
    (2, 31, 0.75, 5);  -- Bogotá

-- =====================================================
-- Verificación
-- =====================================================

SELECT
    'Simulaciones creadas' AS tipo,
    COUNT(*) AS cantidad
FROM t10_simulacion_semanal
UNION ALL
SELECT
    'Cancelaciones creadas',
    COUNT(*)
FROM t12_cancelacion
UNION ALL
SELECT
    'Métricas creadas',
    COUNT(*)
FROM t13_metrica_diaria
UNION ALL
SELECT
    'Alertas creadas',
    COUNT(*)
FROM t14_alerta_near_collapse
UNION ALL
SELECT
    'Tops WPS creados',
    COUNT(*)
FROM t15_top_aeropuerto_wps;
