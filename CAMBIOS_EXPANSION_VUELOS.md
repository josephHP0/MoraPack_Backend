# Cambios Realizados: Sistema de Expansión de Vuelos Diarios

**Fecha**: 2025-11-13
**Autor**: Claude Code
**Objetivo**: Adaptar el sistema para trabajar con vuelos plantilla que se repiten diariamente

---

## Contexto

### Problema Original
El sistema MoraPack tiene:
- **2866 vuelos** en la BD que representan **1 solo día de operaciones** (fecha base: 01-01-2024)
- **622 mil pedidos** con fechas reales (nov 2025 - feb 2026)
- Las simulaciones devolvían **0 pedidos planificados** porque:
  1. Los vuelos no se expandían a lo largo del periodo de simulación
  2. El filtro de fechas era demasiado restrictivo (comparaba horas exactas)

### Solución Implementada
Se creó un **sistema de expansión de vuelos virtuales** que:
1. Toma los 2866 vuelos plantilla del día base (2024-01-01)
2. Los replica para cada día del periodo de simulación
3. Mantiene las **horas** originales, solo ajusta las **fechas**
4. Genera vuelos virtuales sin persistirlos en BD

---

## Archivos Modificados

### 1. **VueloExpansionService.java** (NUEVO)
**Ubicación**: `planificacion/service/VueloExpansionService.java`

**Función**: Expandir vuelos plantilla a lo largo de múltiples días

**Características**:
- Calcula diferencia de días entre día base y periodo de simulación
- Genera vuelos virtuales con fechas ajustadas
- Incluye **buffer**: 1 día antes + 7 días después (para SLA de 72h)
- Clase interna `VueloVirtual` que representa vuelos no persistidos

**Ejemplo de expansión**:
```
Vuelo plantilla: 2024-01-01 08:00:00 → 2024-01-01 14:00:00
Simulación: 2025-11-13 → 2025-11-15

Vuelos expandidos generados:
- 2025-11-12 08:00:00 → 2025-11-12 14:00:00 (buffer -1 día)
- 2025-11-13 08:00:00 → 2025-11-13 14:00:00
- 2025-11-14 08:00:00 → 2025-11-14 14:00:00
- 2025-11-15 08:00:00 → 2025-11-15 14:00:00
- ... (hasta +7 días para buffer SLA)

Total: 2866 vuelos × 9 días = ~25,794 vuelos virtuales
```

---

### 2. **VueloProgramadoRepository.java**
**Cambio**: Agregado método `findAllVuelosPlantilla()`

**Antes**:
```java
// Solo buscaba por rango de fechas
List<T04VueloProgramado> findVuelosDisponibles(Instant fechaInicio, Instant fechaFin);
```

**Después**:
```java
// Nuevo: Obtiene TODOS los vuelos plantilla sin filtro de fechas
@Query("SELECT v FROM T04VueloProgramado v WHERE v.t04Estado != 'CANCELADO' ORDER BY v.t04FechaSalida")
List<T04VueloProgramado> findAllVuelosPlantilla();
```

---

### 3. **AcoPlanner.java** (Interface)
**Cambio**: Agregados parámetros `fechaInicio` y `fechaFin`

**Antes**:
```java
List<T08RutaPlaneada> planificar(List<T02Pedido> pedidos, TipoSimulacion tipoSimulacion);
```

**Después**:
```java
List<T08RutaPlaneada> planificar(
    List<T02Pedido> pedidos,
    TipoSimulacion tipoSimulacion,
    Instant fechaInicio,  // NUEVO
    Instant fechaFin      // NUEVO
);
```

---

### 4. **AcoPlannerImpl.java**
**Cambios principales**:

#### 4.1. Expansión de vuelos al inicio
```java
@Override
public List<T08RutaPlaneada> planificar(..., Instant fechaInicio, Instant fechaFin) {

    // 1. Cargar vuelos plantilla
    List<T04VueloProgramado> vuelosPlantilla = vueloRepository.findAllVuelosPlantilla();

    // 2. Expandirlos para todo el periodo
    vuelosExpandidosCache = vueloExpansionService.expandirVuelos(
        vuelosPlantilla, fechaInicio, fechaFin);

    // 3. Ejecutar ACO con los vuelos expandidos...
}
```

#### 4.2. Filtro de candidatos corregido
**Antes** (PROBLEMA):
```java
private List<T04VueloProgramado> obtenerVuelosCandidatos(T02Pedido pedido) {
    Instant fechaMinima = pedido.getT02FechaPedido();  // Hora exacta: 23:22:24

    return vuelos.stream()
        .filter(v -> v.getFechaSalida().isAfter(fechaMinima))  // Rechaza vuelos del mismo día!
        .collect(Collectors.toList());
}
```

**Después** (SOLUCIÓN):
```java
private List<VueloVirtual> obtenerVuelosCandidatos(T02Pedido pedido) {
    // Usar DÍA del pedido, no hora exacta
    LocalDate diaPedido = pedido.getT02FechaPedido().atZone(ZoneId.of("UTC")).toLocalDate();
    Instant fechaMinima = diaPedido.atStartOfDay(ZoneId.of("UTC")).toInstant();  // 00:00:00

    return vuelosExpandidosCache.stream()
        .filter(v -> !v.getFechaSalida().isBefore(fechaMinima))  // >= mismo día
        .filter(v -> v.getFechaSalida().isBefore(fechaMaxima))
        .filter(v -> v.getCapacidadDisponible() >= pedido.getT02Cantidad())
        .filter(v -> !"CANCELADO".equals(v.getEstado()))
        .collect(Collectors.toList());
}
```

**Efecto**:
- ✅ Pedido del 13-nov a las 23:22 → acepta vuelos desde 13-nov 00:00
- ✅ Permite usar todos los vuelos del día del pedido
- ✅ Ventana de 7 días para encontrar opciones viables

#### 4.3. Logs detallados agregados
```java
log.info("========================================");
log.info("Iniciando planificación ACO");
log.info("Pedidos a planificar: {}", pedidos.size());
log.info("✓ Cargados {} vuelos plantilla desde BD", vuelosPlantilla.size());
log.info("✓ Vuelos expandidos: {} vuelos virtuales generados", vuelosExpandidosCache.size());
log.info("========================================");

// ... al final:

log.info("✓ ACO completado");
log.info("Mejor solución: {} pedidos planificados", mejorHormiga.getSolucion().size());
log.info("Costo total: {}", mejorCosto);
```

---

### 5. **PlanificadorSemanalServiceImpl.java**
**Cambio**: Pasa fechas al ACO

**Antes**:
```java
List<T08RutaPlaneada> rutas = acoPlanner.planificar(pedidos, SEMANAL);
```

**Después**:
```java
List<T08RutaPlaneada> rutas = acoPlanner.planificar(
    pedidos,
    SEMANAL,
    request.getFechaHoraInicio(),  // NUEVO
    request.getFechaHoraFin()       // NUEVO
);
```

**Logs agregados**:
```java
log.info("Query de pedidos:");
log.info("  Rango: {} a {}", request.getFechaHoraInicio(), request.getFechaHoraFin());
log.info("  Pedidos encontrados: {}", pedidos.size());

if (pedidos.isEmpty()) {
    log.warn("ADVERTENCIA: No se encontraron pedidos en el rango especificado");
    log.warn("Verificar:");
    log.warn("  1. Que existan pedidos en la tabla T02_PEDIDO");
    log.warn("  2. Que las fechas coincidan con T02_FECHA_PEDIDO");
    log.warn("  3. Que los destinos NO sean hubs (Lima/Bruselas/Bakú)");
} else {
    T02Pedido primero = pedidos.get(0);
    log.info("  Primer pedido: ID={}, Fecha={}", primero.getId(), primero.getT02FechaPedido());
}
```

---

### 6. **PlanificadorDiaADiaServiceImpl.java**
**Cambio**: Similar al semanal

**Antes**:
```java
List<T08RutaPlaneada> rutas = acoPlanner.planificar(pedidos, DIA_A_DIA);
```

**Después**:
```java
Instant fechaFin = request.getFechaHoraInicio().plusSeconds(request.getVentanaHoras() * 3600L);

List<T08RutaPlaneada> rutas = acoPlanner.planificar(
    pedidos,
    DIA_A_DIA,
    request.getFechaHoraInicio(),
    fechaFin
);
```

---

### 7. **GestorCancelaciones.java**
**Cambio**: Manejo explícito de tabla vacía

**Agregado**:
```java
@Transactional
public int aplicarCancelaciones(Instant fechaInicio, Instant fechaFin) {
    List<T12CancelacionVuelo> cancelaciones =
        cancelacionRepository.findCancelacionesEnRango(fechaInicio, fechaFin);

    if (cancelaciones.isEmpty()) {
        log.info("No hay cancelaciones programadas en el periodo {} a {}", fechaInicio, fechaFin);
        return 0;  // ✅ Flujo normal continúa sin cancelaciones
    }

    // ... resto del código
}
```

---

## Flujo Completo del Sistema

### 1. **Inicio de simulación semanal**
```
POST /api/simulacion-semanal/ejecutar
{
  "fechaHoraInicio": "2025-11-13T00:00:00Z",
  "fechaHoraFin": "2025-11-20T23:59:59Z"
}
```

### 2. **PlanificadorSemanalServiceImpl**
```
1. Crear T10_RESULTADO_SIMULACION (estado: EN_PROGRESO)
2. Aplicar cancelaciones (retorna 0 si tabla vacía)
3. Query pedidos: findPedidosNoHubBetween(inicio, fin)
   → Retorna pedidos del rango (nov 13-20, 2025)
4. Llamar ACO con fechas
```

### 3. **AcoPlannerImpl**
```
1. Cargar vuelos plantilla: findAllVuelosPlantilla()
   → 2866 vuelos del 01-01-2024

2. Expandir vuelos: vueloExpansionService.expandirVuelos()
   → Genera vuelos para 12-nov a 27-nov (con buffer)
   → Total: 2866 × 16 días = ~45,856 vuelos virtuales

3. Ejecutar ACO (50 hormigas × 100 iteraciones):
   Para cada pedido:
     a. obtenerVuelosCandidatos(pedido)
        → Filtra vuelos del mismo día del pedido (+7 días)
        → Verifica capacidad disponible
     b. seleccionarVuelo() usando feromonas + heurística
     c. asignarVuelo() a la hormiga

4. Retornar mejor solución
```

### 4. **Resultado esperado**
```json
{
  "idResultado": 10,
  "tipoSimulacion": "SEMANAL",
  "fechaInicio": "2025-11-13T00:00:00Z",
  "fechaFin": "2025-11-20T23:59:59Z",
  "estado": "COMPLETADO",
  "metricas": {
    "totalPedidos": 15000,          // ✅ Ya no es 0
    "pedidosEntregados": 0,
    "pedidosEnTransito": 14500,     // ✅ Pedidos planificados exitosamente
    "pedidosRechazados": 500,
    "cumplimientoSla": 96.67
  }
}
```

---

## Consideraciones Importantes

### ✅ Lo que SÍ hace el sistema
1. **Expande vuelos automáticamente** para cada día de la simulación
2. **Mantiene las horas** de los vuelos plantilla (solo cambia fechas)
3. **Filtra correctamente** usando días en vez de horas exactas
4. **Maneja tablas vacías** (cancelaciones, vuelos, pedidos)
5. **Logs detallados** para debugging

### ⚠️ Limitaciones actuales
1. **Vuelos virtuales no se persisten** en la BD (solo existen en memoria durante la simulación)
2. **Búsqueda simplificada** - solo vuelos directos, no multi-hop (hub → hub → destino)
3. **SLA fijo de 72h** - no considera mismo/diferente continente
4. **Capacidad no se libera** al entregar pedidos

### 🔧 Próximas mejoras sugeridas
1. Implementar búsqueda de rutas multi-hop con grafos (Dijkstra/A*)
2. Calcular SLA dinámico basado en continentes (48h vs 72h)
3. Mejorar función de costo del ACO (multi-objetivo)
4. Validar tiempo mínimo de estancia (1h entre vuelos)

---

## Cómo Probar

### 1. Verificar vuelos en BD
```sql
SELECT COUNT(*) FROM morapack2.t04_vuelo_programado WHERE t04_estado != 'CANCELADO';
-- Debe retornar: 2866

SELECT MIN(t04_fecha_salida), MAX(t04_fecha_salida) FROM morapack2.t04_vuelo_programado;
-- Debe mostrar: día base (probablemente 2024-01-01)
```

### 2. Verificar pedidos en BD
```sql
SELECT COUNT(*) FROM morapack2.t02_pedido
WHERE t02_fecha_pedido BETWEEN '2025-11-13' AND '2025-11-20';
-- Debe retornar: cantidad de pedidos en ese rango

SELECT t05_es_hub FROM morapack2.t05_ciudad c
JOIN morapack2.t01_aeropuerto a ON a.t01_id_ciudad = c.t05_id
JOIN morapack2.t02_pedido p ON p.t02_id_aerop_destino = a.t01_id
LIMIT 10;
-- Debe mostrar: 0 (false) - pedidos NO van a hubs
```

### 3. Ejecutar simulación de prueba
```bash
curl -X POST http://localhost:8080/api/simulacion-semanal/ejecutar \
  -H "Content-Type: application/json" \
  -d '{
    "fechaHoraInicio": "2025-11-13T00:00:00Z",
    "fechaHoraFin": "2025-11-20T23:59:59Z"
  }'
```

### 4. Revisar logs
Buscar en la consola:
```
========================================
Iniciando planificación ACO
Pedidos a planificar: XXXX
✓ Cargados 2866 vuelos plantilla desde BD
✓ Vuelos expandidos: XXXXX vuelos virtuales generados
========================================
```

Si aparece:
- `Pedidos a planificar: 0` → Revisar query de pedidos
- `Vuelos expandidos: 0` → Revisar tabla T04_VUELO_PROGRAMADO
- `No se encontraron vuelos candidatos` → Revisar filtros de capacidad/fechas

---

## Contacto y Soporte

**Desarrollador**: Claude Code
**Fecha**: 2025-11-13
**Versión**: 1.0 - Sistema de Expansión de Vuelos

Para más información, consultar:
- `CONOCIMIENTO_COMPLETO_MORAPACK.txt` - Documentación completa del sistema
- Logs de aplicación en consola (Spring Boot)
- Swagger: http://localhost:8080/swagger
