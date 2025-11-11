-- =====================================================
-- Script de Limpieza/Rollback
-- MoraPack Backend - Versión 1.0
-- =====================================================

USE morapack2;

-- =====================================================
-- ADVERTENCIA
-- Este script elimina TODAS las tablas de simulación
-- y sus datos. Usar con precaución.
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Eliminar tablas en orden inverso (para respetar FK)
DROP TABLE IF EXISTS t15_top_aeropuerto_wps;
DROP TABLE IF EXISTS t14_alerta_near_collapse;
DROP TABLE IF EXISTS t13_metrica_diaria;
DROP TABLE IF EXISTS t12_cancelacion;
DROP TABLE IF EXISTS t10_simulacion_semanal;

SET FOREIGN_KEY_CHECKS = 1;

-- Verificación
SELECT 'Tablas de simulación eliminadas' AS status;

-- =====================================================
-- Limpieza de Datos de Prueba (sin eliminar tablas)
-- =====================================================
/*
TRUNCATE TABLE t15_top_aeropuerto_wps;
TRUNCATE TABLE t14_alerta_near_collapse;
TRUNCATE TABLE t13_metrica_diaria;
TRUNCATE TABLE t12_cancelacion;
TRUNCATE TABLE t10_simulacion_semanal;

SELECT 'Datos de prueba eliminados, tablas preservadas' AS status;
*/
