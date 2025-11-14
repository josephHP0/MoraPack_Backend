# Troubleshooting: Simulación Retorna 0 Pedidos

**Problema**: La simulación se ejecuta correctamente pero retorna `totalPedidos: 0`

---

## Endpoint de Debug

Antes de ejecutar una simulación, usa este endpoint para verificar cuántos pedidos hay disponibles:

```bash
GET /api/simulacion-semanal/debug/pedidos?fechaInicio=2025-11-13T00:00:00Z&fechaFin=2025-11-14T00:00:00Z
```

### Response Ejemplo

```json
{
  "fechaInicio": "2025-11-13T00:00:00Z",
  "fechaFin": "2025-11-14T00:00:00Z",
  "totalPedidosEnBD": 622000,
  "pedidosEnRango": 5420,
  "pedidosNoHub": 4950,
  "pedidosHubExcluidos": 470,
  "distribucionPorDia": [
    {
      "fecha": "2025-11-13",
      "cantidad": 4950
    }
  ],
  "muestraPedidos": [
    {
      "id": 10001,
      "idCadena": "000010001-20251113-08-30-EBBR-250-0001234",
      "fechaPedido": "2025-11-13T08:30:00Z",
      "cantidad": 250,
      "destino": "Bruselas",
      "esHub": false
    }
  ]
}
```

---

## Diagnóstico Paso a Paso

### 1. Verificar que hay pedidos en la BD

```sql
SELECT COUNT(*) FROM morapack2.t02_pedido;
```

**Resultado esperado**: 622,000 pedidos (o más)

**Si es 0**: No hay pedidos en la BD → Cargar datos de pedidos

---

### 2. Verificar fechas de los pedidos

```sql
SELECT
    MIN(T02_FECHA_PEDIDO) as primera_fecha,
    MAX(T02_FECHA_PEDIDO) as ultima_fecha,
    COUNT(*) as total
FROM morapack2.t02_pedido;
```

**Resultado esperado**:
```
primera_fecha: 2025-11-01 (o similar)
ultima_fecha: 2026-02-28 (o similar)
total: 622000
```

**Si las fechas no coinciden**: Los pedidos están en otro rango de fechas
→ Ajusta fechaInicio y fechaFin en tu request

---

### 3. Verificar pedidos en tu rango específico

```sql
SELECT COUNT(*)
FROM morapack2.t02_pedido
WHERE T02_FECHA_PEDIDO BETWEEN '2025-11-13 00:00:00' AND '2025-11-14 00:00:00';
```

**Si es 0**: No hay pedidos en ese rango específico (13-14 nov)
→ Usa un rango más amplio (ej: 1 semana completa)

```json
{
  "fechaHoraInicio": "2025-11-01T00:00:00Z",
  "fechaHoraFin": "2025-11-07T23:59:59Z"
}
```

---

### 4. Verificar que los destinos NO son hubs

```sql
SELECT
    c.T05_NOMBRE as ciudad_destino,
    c.T05_ES_HUB as es_hub,
    COUNT(*) as cantidad_pedidos
FROM morapack2.t02_pedido p
JOIN morapack2.t01_aeropuerto a ON p.T02_ID_AEROP_DESTINO = a.T01_ID_AEROPUERTO
JOIN morapack2.t05_ciudad c ON a.T01_ID_CIUDAD = c.T05_ID_CIUDAD
WHERE p.T02_FECHA_PEDIDO BETWEEN '2025-11-13' AND '2025-11-14'
GROUP BY c.T05_NOMBRE, c.T05_ES_HUB
ORDER BY cantidad_pedidos DESC;
```

**Resultado esperado**:
```
ciudad_destino | es_hub | cantidad_pedidos
---------------|--------|------------------
París          | 0      | 1200
Berlín         | 0      | 980
...
Lima           | 1      | 50  (estos se excluyen)
Bruselas       | 1      | 30  (estos se excluyen)
```

**Si todos son hub = 1**: Todos los pedidos van a hubs y se excluyen
→ Revisar lógica de negocio (¿por qué todos van a hubs?)

---

### 5. Verificar vuelos expandidos

**Revisar logs de la aplicación**:
```
✓ Cargados 2866 vuelos plantilla desde BD
✓ Vuelos expandidos: 25794 vuelos virtuales generados
```

**Si vuelos expandidos = 0**:
- Verificar que hay vuelos en `t04_vuelo_programado`
- Ver sección "Verificar vuelos" abajo

---

### 6. Verificar que ACO encuentra candidatos

**Revisar logs**:
```
Pedido 10001: 0 vuelos candidatos encontrados (dia=2025-11-13, cantidad=250)
```

**Posibles causas**:
1. **Capacidad insuficiente**: Los vuelos no tienen espacio para la cantidad del pedido
2. **Fechas incorrectas**: Los vuelos expandidos no cubren el día del pedido
3. **Vuelos cancelados**: Todos los vuelos están marcados como CANCELADO

---

## Verificaciones Adicionales

### Verificar vuelos en BD

```sql
SELECT COUNT(*) FROM morapack2.t04_vuelo_programado WHERE t04_estado != 'CANCELADO';
```

**Resultado esperado**: 2866 vuelos

**Si es 0**: No hay vuelos disponibles → Cargar datos de vuelos

---

### Verificar fechas de vuelos

```sql
SELECT
    MIN(T04_FECHA_SALIDA) as primera_salida,
    MAX(T04_FECHA_SALIDA) as ultima_salida
FROM morapack2.t04_vuelo_programado;
```

**Resultado esperado**:
```
primera_salida: 2024-01-01 00:xx:xx (fecha base)
ultima_salida: 2024-01-01 23:xx:xx (mismo día)
```

**Nota**: Las fechas exactas no importan (el sistema las expande). Solo importan las horas.

---

### Verificar capacidad de vuelos

```sql
SELECT
    AVG(T04_CAPACIDAD_TOTAL) as capacidad_promedio,
    MIN(T04_CAPACIDAD_TOTAL) as capacidad_minima,
    MAX(T04_CAPACIDAD_TOTAL) as capacidad_maxima
FROM morapack2.t04_vuelo_programado;
```

**Verificar**: La capacidad mínima debe ser mayor que las cantidades de los pedidos

---

## Soluciones Comunes

### Problema: "No hay pedidos en el rango 13-14 nov"

**Solución**: Usa un rango más amplio

```json
{
  "fechaHoraInicio": "2025-11-01T00:00:00Z",
  "fechaHoraFin": "2025-11-30T23:59:59Z",
  "duracionBloqueHoras": 24
}
```

---

### Problema: "Todos los pedidos van a hubs (Lima/Bruselas/Bakú)"

**Verificar configuración de hubs**:
```sql
SELECT T05_NOMBRE, T05_ES_HUB
FROM morapack2.t05_ciudad
WHERE T05_ES_HUB = 1;
```

**Debe retornar solo 3 ciudades**: Lima, Bruselas, Bakú

**Si aparecen más ciudades**: Revisar y corregir campo T05_ES_HUB

---

### Problema: "Capacidad insuficiente"

**Verificar cantidades de pedidos**:
```sql
SELECT
    AVG(T02_CANTIDAD) as promedio,
    MAX(T02_CANTIDAD) as maximo
FROM morapack2.t02_pedido;
```

**Comparar con capacidad de vuelos**:
```sql
SELECT AVG(T04_CAPACIDAD_TOTAL) FROM morapack2.t04_vuelo_programado;
```

**Si promedio pedido > capacidad promedio vuelo**: Problema de diseño
→ Ajustar capacidades o permitir consolidación de pedidos

---

## Checklist Rápido

Antes de ejecutar una simulación, verificar:

- [ ] ✅ Hay pedidos en la BD (622k)
- [ ] ✅ Las fechas de los pedidos cubren el rango de simulación
- [ ] ✅ Los destinos NO son todos hubs
- [ ] ✅ Hay 2866 vuelos en la BD
- [ ] ✅ Los vuelos NO están todos CANCELADOS
- [ ] ✅ La capacidad de vuelos es suficiente
- [ ] ✅ El rango de simulación es razonable (no solo 1 hora)

---

## Uso del Endpoint de Debug

### Caso 1: Verificar antes de simular

```bash
# 1. Verificar pedidos disponibles
curl "http://localhost:8080/api/simulacion-semanal/debug/pedidos?fechaInicio=2025-11-13T00:00:00Z&fechaFin=2025-11-20T00:00:00Z"

# 2. Si hay pedidos (pedidosNoHub > 0), ejecutar simulación
curl -X POST http://localhost:8080/api/simulacion-semanal/ejecutar \
  -H "Content-Type: application/json" \
  -d '{
    "fechaHoraInicio": "2025-11-13T00:00:00Z",
    "fechaHoraFin": "2025-11-20T23:59:59Z",
    "duracionBloqueHoras": 24
  }'
```

### Caso 2: Encontrar rango con pedidos

```bash
# Probar diferentes rangos hasta encontrar pedidos
curl "http://localhost:8080/api/simulacion-semanal/debug/pedidos?fechaInicio=2025-11-01T00:00:00Z&fechaFin=2025-11-07T23:59:59Z"

curl "http://localhost:8080/api/simulacion-semanal/debug/pedidos?fechaInicio=2025-12-01T00:00:00Z&fechaFin=2025-12-07T23:59:59Z"
```

---

## Contacto y Soporte

Si después de seguir estos pasos sigues teniendo problemas:

1. Ejecuta el endpoint `/debug/pedidos`
2. Revisa los logs de la aplicación
3. Ejecuta las queries SQL de verificación
4. Comparte los resultados para diagnóstico

**Fecha**: 2025-11-13
**Versión**: 2.0
