# Guía de Integración Frontend - API MoraPack

**Fecha**: 2025-11-13
**Version**: 2.0 - Respuestas Detalladas con Bloques

---

## Cambios Principales

### ✅ Implementado
1. **Simulación por bloques** usando `duracionBloqueHoras`
2. **Response detallado** con información completa de vuelos, pedidos y rutas
3. **Estadísticas extendidas** para visualización en dashboard

---

## Endpoint: Ejecutar Simulación Semanal

### `POST /api/simulacion-semanal/ejecutar`

### Request

#### Opción 1: Batch Completo (sin bloques)
```json
{
  "fechaHoraInicio": "2025-11-13T00:00:00Z",
  "fechaHoraFin": "2025-11-20T23:59:59Z",
  "duracionBloqueHoras": null
}
```

#### Opción 2: Simulación por Bloques de 24 horas
```json
{
  "fechaHoraInicio": "2025-11-13T00:00:00Z",
  "fechaHoraFin": "2025-11-20T23:59:59Z",
  "duracionBloqueHoras": 24
}
```

### Response Detallado

```json
{
  "idResultado": 10,
  "tipoSimulacion": "SEMANAL",
  "fechaInicio": "2025-11-13T00:00:00Z",
  "fechaFin": "2025-11-20T23:59:59Z",
  "fechaEjecucion": "2025-11-13T10:30:45Z",
  "duracionMs": 45230,
  "estado": "COMPLETADO",
  "mensaje": "Simulación completada: 7 bloques, 15234 pedidos planificados",

  "duracionBloqueHoras": 24,
  "numeroBloquesEjecutados": 7,

  "metricas": {
    "totalPedidos": 15234,
    "pedidosEntregados": 0,
    "pedidosEnTransito": 14850,
    "pedidosRechazados": 384,
    "cumplimientoSla": 97.48,
    "ocupacionPromedio": 72.5,
    "vuelosUtilizados": 1250,
    "vuelosCancelados": 5
  },

  "vuelos": [
    {
      "vueloId": 1234,
      "codigoOrigenICAO": "SPJC",
      "nombreOrigenCiudad": "Lima",
      "codigoDestinoICAO": "EBBR",
      "nombreDestinoCiudad": "Bruselas",
      "fechaSalida": "2025-11-13T08:00:00Z",
      "fechaLlegada": "2025-11-13T20:00:00Z",
      "matriculaAvion": "N12345",
      "modeloAvion": "Boeing 777F",
      "capacidadTotal": 1000,
      "capacidadOcupada": 850,
      "capacidadDisponible": 150,
      "porcentajeOcupacion": 85.0,
      "estadoCapacidad": "NORMAL",
      "pedidosAsignados": [
        {
          "pedidoId": 5001,
          "idCadena": "000005001-20251113-08-30-EBBR-250-0001234",
          "cantidad": 250,
          "destino": "Bruselas",
          "esVueloFinal": true
        },
        {
          "pedidoId": 5002,
          "idCadena": "000005002-20251113-09-15-LFPG-300-0001235",
          "cantidad": 300,
          "destino": "París",
          "esVueloFinal": false
        },
        {
          "pedidoId": 5003,
          "idCadena": "000005003-20251113-10-00-EBBR-300-0001236",
          "cantidad": 300,
          "destino": "Bruselas",
          "esVueloFinal": true
        }
      ]
    },
    {
      "vueloId": 1235,
      "codigoOrigenICAO": "EBBR",
      "nombreOrigenCiudad": "Bruselas",
      "codigoDestinoICAO": "LFPG",
      "nombreDestinoCiudad": "París",
      "fechaSalida": "2025-11-13T22:00:00Z",
      "fechaLlegada": "2025-11-13T23:30:00Z",
      "matriculaAvion": "F-GZNA",
      "modeloAvion": "Airbus A330-200F",
      "capacidadTotal": 600,
      "capacidadOcupada": 300,
      "capacidadDisponible": 300,
      "porcentajeOcupacion": 50.0,
      "estadoCapacidad": "NORMAL",
      "pedidosAsignados": [
        {
          "pedidoId": 5002,
          "idCadena": "000005002-20251113-09-15-LFPG-300-0001235",
          "cantidad": 300,
          "destino": "París",
          "esVueloFinal": true
        }
      ]
    }
  ],

  "pedidos": [
    {
      "pedidoId": 5001,
      "idCadena": "000005001-20251113-08-30-EBBR-250-0001234",
      "fechaPedido": "2025-11-13T08:30:00Z",
      "cantidad": 250,
      "clienteNombre": "Cliente ABC Corp",
      "destinoCodigoICAO": "EBBR",
      "destinoCiudad": "Bruselas",
      "estado": "PENDIENTE",
      "cumpleSla": true,
      "fechaEntregaEstimada": "2025-11-13T22:00:00Z",
      "fechaLimiteSla": "2025-11-16T10:30:00Z",
      "tramos": [
        {
          "ordenEnRuta": 1,
          "vueloId": 1234,
          "origenICAO": "SPJC",
          "origenCiudad": "Lima",
          "destinoICAO": "EBBR",
          "destinoCiudad": "Bruselas",
          "fechaSalida": "2025-11-13T08:00:00Z",
          "fechaLlegada": "2025-11-13T20:00:00Z",
          "esVueloFinal": true,
          "cantidadProductos": 250
        }
      ]
    },
    {
      "pedidoId": 5002,
      "idCadena": "000005002-20251113-09-15-LFPG-300-0001235",
      "fechaPedido": "2025-11-13T09:15:00Z",
      "cantidad": 300,
      "clienteNombre": "Cliente XYZ Ltd",
      "destinoCodigoICAO": "LFPG",
      "destinoCiudad": "París",
      "estado": "PENDIENTE",
      "cumpleSla": true,
      "fechaEntregaEstimada": "2025-11-14T01:30:00Z",
      "fechaLimiteSla": "2025-11-16T11:15:00Z",
      "tramos": [
        {
          "ordenEnRuta": 1,
          "vueloId": 1234,
          "origenICAO": "SPJC",
          "origenCiudad": "Lima",
          "destinoICAO": "EBBR",
          "destinoCiudad": "Bruselas",
          "fechaSalida": "2025-11-13T08:00:00Z",
          "fechaLlegada": "2025-11-13T20:00:00Z",
          "esVueloFinal": false,
          "cantidadProductos": 300
        },
        {
          "ordenEnRuta": 2,
          "vueloId": 1235,
          "origenICAO": "EBBR",
          "origenCiudad": "Bruselas",
          "destinoICAO": "LFPG",
          "destinoCiudad": "París",
          "fechaSalida": "2025-11-13T22:00:00Z",
          "fechaLlegada": "2025-11-13T23:30:00Z",
          "esVueloFinal": true,
          "cantidadProductos": 300
        }
      ]
    }
  ],

  "estadisticas": {
    "totalVuelosDisponibles": 1250,
    "vuelosUtilizados": 1250,
    "vuelosNoUtilizados": 0,
    "porcentajeUtilizacionVuelos": 100.0,
    "ocupacionPromedioTodosVuelos": 72.5,
    "ocupacionPromedioVuelosUtilizados": 72.5,
    "vuelosConSobrecarga": 12,
    "pedidosPendientes": 14850,
    "pedidosEnTransito": 0,
    "pedidosEntregados": 0,
    "pedidosRechazados": 384,
    "pedidosCumplenSla": 14850,
    "pedidosNoCumplenSla": 384,
    "porcentajeCumplimientoSla": 97.48
  }
}
```

---

## Casos de Uso para el Frontend

### 1. Vista de Dashboard Principal

**Datos a mostrar**:
```javascript
// Métricas generales
const metricas = response.metricas;

<Dashboard>
  <MetricCard title="Total Pedidos" value={metricas.totalPedidos} />
  <MetricCard title="Planificados" value={metricas.pedidosEnTransito} color="green" />
  <MetricCard title="Rechazados" value={metricas.pedidosRechazados} color="red" />
  <MetricCard title="Cumplimiento SLA" value={`${metricas.cumplimientoSla}%`} />
  <MetricCard title="Vuelos Utilizados" value={metricas.vuelosUtilizados} />
  <MetricCard title="Ocupación Promedio" value={`${metricas.ocupacionPromedio}%`} />
</Dashboard>
```

### 2. Mapa de Vuelos (Visualización de Rutas)

**Datos a usar**:
```javascript
const vuelos = response.vuelos;

// Para cada vuelo, dibujar línea en mapa
vuelos.forEach(vuelo => {
  const origen = {
    codigo: vuelo.codigoOrigenICAO,
    ciudad: vuelo.nombreOrigenCiudad
  };
  const destino = {
    codigo: vuelo.codigoDestinoICAO,
    ciudad: vuelo.nombreDestinoCiudad
  };

  // Dibujar línea
  drawFlightPath(origen, destino, {
    color: vuelo.porcentajeOcupacion > 90 ? 'red' : 'green',
    width: 2,
    label: `${vuelo.porcentajeOcupacion}% ocupado`
  });

  // Mostrar info en tooltip
  onHover(() => {
    showTooltip(`
      Vuelo: ${vuelo.matriculaAvion}
      Modelo: ${vuelo.modeloAvion}
      Ocupación: ${vuelo.capacidadOcupada}/${vuelo.capacidadTotal}
      Pedidos: ${vuelo.pedidosAsignados.length}
    `);
  });
});
```

### 3. Tabla de Vuelos con Detalle

**Datos a mostrar**:
```javascript
<Table>
  <thead>
    <tr>
      <th>Vuelo</th>
      <th>Ruta</th>
      <th>Fecha/Hora</th>
      <th>Avión</th>
      <th>Ocupación</th>
      <th>Pedidos</th>
    </tr>
  </thead>
  <tbody>
    {response.vuelos.map(vuelo => (
      <tr key={vuelo.vueloId}>
        <td>{vuelo.vueloId}</td>
        <td>{vuelo.codigoOrigenICAO} → {vuelo.codigoDestinoICAO}</td>
        <td>{formatDateTime(vuelo.fechaSalida)}</td>
        <td>{vuelo.matriculaAvion} ({vuelo.modeloAvion})</td>
        <td>
          <ProgressBar
            value={vuelo.porcentajeOcupacion}
            color={vuelo.estadoCapacidad === 'SOBRECARGA' ? 'red' : 'green'}
          />
          <span>{vuelo.capacidadOcupada}/{vuelo.capacidadTotal}</span>
        </td>
        <td>
          <button onClick={() => showPedidosAsignados(vuelo.pedidosAsignados)}>
            {vuelo.pedidosAsignados.length} pedidos
          </button>
        </td>
      </tr>
    ))}
  </tbody>
</Table>
```

### 4. Detalle de Pedido con Tracking de Ruta

**Datos a usar**:
```javascript
const pedido = response.pedidos.find(p => p.pedidoId === selectedId);

<PedidoDetail>
  <h3>Pedido {pedido.idCadena}</h3>

  <InfoSection>
    <p>Cliente: {pedido.clienteNombre}</p>
    <p>Cantidad: {pedido.cantidad} unidades</p>
    <p>Destino: {pedido.destinoCiudad} ({pedido.destinoCodigoICAO})</p>
    <p>Estado: <Badge color={getColorByEstado(pedido.estado)}>{pedido.estado}</Badge></p>
    <p>SLA: {pedido.cumpleSla ? '✅ Cumple' : '❌ No cumple'}</p>
    <p>Fecha límite: {formatDateTime(pedido.fechaLimiteSla)}</p>
    <p>Entrega estimada: {formatDateTime(pedido.fechaEntregaEstimada)}</p>
  </InfoSection>

  <RutaTracking>
    <Timeline>
      {pedido.tramos.map((tramo, index) => (
        <TimelineItem key={index} active={index === 0}>
          <h4>Tramo {tramo.ordenEnRuta}</h4>
          <p>Vuelo #{tramo.vueloId}</p>
          <p>{tramo.origenCiudad} → {tramo.destinoCiudad}</p>
          <p>Salida: {formatDateTime(tramo.fechaSalida)}</p>
          <p>Llegada: {formatDateTime(tramo.fechaLlegada)}</p>
          {tramo.esVueloFinal && <Badge>Destino Final</Badge>}
        </TimelineItem>
      ))}
    </Timeline>
  </RutaTracking>
</PedidoDetail>
```

### 5. Estadísticas Avanzadas

**Datos a usar**:
```javascript
const stats = response.estadisticas;

<StatsPanel>
  <Section title="Vuelos">
    <Stat label="Total disponibles" value={stats.totalVuelosDisponibles} />
    <Stat label="Utilizados" value={stats.vuelosUtilizados} />
    <Stat label="No utilizados" value={stats.vuelosNoUtilizados} />
    <Stat label="Tasa de utilización" value={`${stats.porcentajeUtilizacionVuelos}%`} />
    <Stat label="Vuelos sobrecargados" value={stats.vuelosConSobrecarga} color="warning" />
  </Section>

  <Section title="Ocupación">
    <Stat label="Promedio todos los vuelos" value={`${stats.ocupacionPromedioTodosVuelos}%`} />
    <Stat label="Promedio vuelos utilizados" value={`${stats.ocupacionPromedioVuelosUtilizados}%`} />
  </Section>

  <Section title="SLA">
    <PieChart data={[
      { label: 'Cumplen SLA', value: stats.pedidosCumplenSla, color: 'green' },
      { label: 'No cumplen SLA', value: stats.pedidosNoCumplenSla, color: 'red' }
    ]} />
  </Section>
</StatsPanel>
```

---

## Simulación por Bloques vs Batch

### Batch Completo (`duracionBloqueHoras: null`)
```json
{
  "duracionBloqueHoras": null,
  "numeroBloquesEjecutados": 1
}
```
- **Ventaja**: Solución óptima global
- **Desventaja**: Más lento para muchos pedidos
- **Uso**: Simulaciones históricas, análisis completo

### Por Bloques (`duracionBloqueHoras: 24`)
```json
{
  "duracionBloqueHoras": 24,
  "numeroBloquesEjecutados": 7
}
```
- **Ventaja**: Más rápido, checkpoints intermedios
- **Desventaja**: Soluciones sub-óptimas
- **Uso**: Operaciones diarias, simulaciones en tiempo real

---

## Códigos de Estado

### Estados de Pedido
- `PENDIENTE`: Planificado pero no ha partido
- `EN_TRANSITO`: En proceso de envío
- `ENTREGADO`: Completado exitosamente
- `RECHAZADO`: No se pudo planificar

### Estados de Simulación
- `EN_PROGRESO`: Ejecutándose
- `COMPLETADO`: Finalizada con éxito
- `ERROR`: Terminó con error

### Estados de Capacidad de Vuelo
- `NORMAL`: Ocupación <= 100%
- `SOBRECARGA`: Ocupación > 100%

---

## Ejemplos de Filtrado en Frontend

### Filtrar vuelos con alta ocupación
```javascript
const vuelosAlta Ocupacion = response.vuelos.filter(v => v.porcentajeOcupacion > 80);
```

### Filtrar pedidos rechazados
```javascript
const pedidosRechazados = response.pedidos.filter(p => p.estado === 'RECHAZADO');
```

### Agrupar pedidos por destino
```javascript
const pedidosPorDestino = response.pedidos.reduce((acc, pedido) => {
  const ciudad = pedido.destinoCiudad;
  if (!acc[ciudad]) acc[ciudad] = [];
  acc[ciudad].push(pedido);
  return acc;
}, {});
```

### Encontrar pedidos que no cumplen SLA
```javascript
const pedidosFueraSLA = response.pedidos.filter(p => !p.cumpleSla);
```

---

## Notas Importantes

1. **Todos los vuelos listados tienen pedidos asignados** - No se incluyen vuelos vacíos
2. **Las fechas están en formato ISO 8601 UTC** - Ajustar a zona horaria local en frontend
3. **Los tramos están ordenados por `ordenEnRuta`** - Ya vienen en orden correcto (1, 2, 3...)
4. **`esVueloFinal: true`** indica el último tramo de la ruta del pedido
5. **Los IDs de vuelo (`vueloId`) corresponden a las plantillas** - Múltiples vuelos virtuales comparten el mismo ID base

---

## Contacto

**Desarrollador**: Claude Code
**Fecha**: 2025-11-13
**Versión API**: 2.0
