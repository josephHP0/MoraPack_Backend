# Sistema de Planificación ACO - MoraPack Backend

## Resumen de Implementación

Se ha implementado un sistema completo de planificación de pedidos usando el algoritmo **Ant Colony Optimization (ACO)** con dos modos de operación:

1. **Simulación Semanal (Batch)**: Planifica todos los pedidos de una semana completa
2. **Operación Día a Día (Rolling)**: Planifica pedidos en ventanas cortas (1-2 horas)

---

## 1. Estructura Creada

### Scripts SQL (BD/BD/)
- `morapack2_t08_ruta_planeada.sql` - Rutas planificadas por ACO
- `morapack2_t09_tramo_asignado.sql` - Tramos/vuelos asignados a cada pedido
- `morapack2_t10_resultado_simulacion.sql` - Resultados de simulaciones
- `morapack2_t11_metricas_simulacion.sql` - Métricas agregadas
- `morapack2_t12_cancelacion_vuelo.sql` - Cancelaciones programadas

### Paquetes Java Creados

```
com.morapack.nuevomoraback/
├── common/
│   └── repository/              # Repositories para entidades base (T01-T07)
│       ├── AeropuertoRepository.java
│       ├── PedidoRepository.java
│       ├── VueloProgramadoRepository.java
│       ├── RutaRepository.java
│       ├── CiudadRepository.java
│       ├── ClienteRepository.java
│       └── AvionRepository.java
│
└── planificacion/
    ├── aco/                     # Núcleo del algoritmo ACO
    │   ├── AcoPlanner.java
    │   ├── AcoPlannerImpl.java
    │   ├── AcoParameters.java
    │   ├── Ant.java
    │   ├── PheromoneMatrix.java
    │   └── HeuristicCalculator.java
    │
    ├── domain/                  # Entidades JPA (T08-T12)
    │   ├── T08RutaPlaneada.java
    │   ├── T09TramoAsignado.java
    │   ├── T10ResultadoSimulacion.java
    │   ├── T11MetricasSimulacion.java
    │   └── T12CancelacionVuelo.java
    │
    ├── repository/              # Repositories para planificación
    │   ├── T08RutaPlaneadaRepository.java
    │   ├── T09TramoAsignadoRepository.java
    │   ├── T10ResultadoSimulacionRepository.java
    │   ├── T11MetricasSimulacionRepository.java
    │   └── T12CancelacionVueloRepository.java
    │
    ├── dto/                     # DTOs para requests/responses
    │   ├── SimulacionSemanalRequest.java
    │   ├── SimulacionSemanalResponse.java
    │   ├── PlanificacionDiaADiaRequest.java
    │   ├── PlanificacionDiaADiaResponse.java
    │   ├── MetricasDTO.java
    │   └── EstadoPedidoDTO.java
    │
    ├── service/                 # Servicios de negocio
    │   ├── PlanificadorSemanalService.java
    │   ├── PlanificadorSemanalServiceImpl.java
    │   ├── PlanificadorDiaADiaService.java
    │   ├── PlanificadorDiaADiaServiceImpl.java
    │   ├── ValidadorReglas.java
    │   ├── CalculadorSLA.java
    │   └── GestorCancelaciones.java
    │
    └── controller/              # Controladores REST
        ├── PlanificacionSemanalController.java
        └── PlanificacionDiaADiaController.java
```

---

## 2. Pasos para Ejecutar

### Paso 1: Crear las Tablas en MySQL

Ejecuta los scripts SQL en el orden correcto:

```bash
cd "C:\Lhia\Cursos Catolica\DP1\Proyecto\BD\BD"

# Ejecutar en MySQL
mysql -u root -p morapack2 < morapack2_t08_ruta_planeada.sql
mysql -u root -p morapack2 < morapack2_t09_tramo_asignado.sql
mysql -u root -p morapack2 < morapack2_t10_resultado_simulacion.sql
mysql -u root -p morapack2 < morapack2_t11_metricas_simulacion.sql
mysql -u root -p morapack2 < morapack2_t12_cancelacion_vuelo.sql
```

O desde MySQL Workbench, ejecuta cada script individualmente.

### Paso 2: Compilar el Proyecto

```bash
cd "C:\Lhia\Cursos Catolica\DP1\Proyecto\MoraPack_Backend"
mvnw.cmd clean compile
```

### Paso 3: Ejecutar el Backend

```bash
mvnw.cmd spring-boot:run
```

El servidor arrancará en `http://localhost:8080`

---

## 3. Endpoints REST Disponibles

### Documentación Swagger
Accede a: `http://localhost:8080/swagger`

### Simulación Semanal

#### POST `/api/simulacion-semanal/ejecutar`
Ejecuta una simulación semanal batch

**Request Body:**
```json
{
  "fechaHoraInicio": "2024-01-01T00:00:00Z",
  "fechaHoraFin": "2024-01-07T23:59:59Z",
  "duracionBloqueHoras": null
}
```

**Response:**
```json
{
  "idResultado": 1,
  "tipoSimulacion": "SEMANAL",
  "fechaInicio": "2024-01-01T00:00:00Z",
  "fechaFin": "2024-01-07T23:59:59Z",
  "fechaEjecucion": "2025-01-12T10:30:00Z",
  "duracionMs": 45230,
  "estado": "COMPLETADO",
  "mensaje": "Simulación completada exitosamente",
  "metricas": {
    "totalPedidos": 1500,
    "pedidosEntregados": 0,
    "pedidosEnTransito": 1450,
    "pedidosRechazados": 50,
    "cumplimientoSla": 96.67,
    "ocupacionPromedio": null,
    "vuelosUtilizados": 320,
    "vuelosCancelados": 5
  }
}
```

#### GET `/api/simulacion-semanal/resultado/{id}`
Consulta el resultado de una simulación por ID

### Operación Día a Día

#### POST `/api/dia-a-dia/planificar`
Planifica pedidos en una ventana corta

**Request Body:**
```json
{
  "fechaHoraInicio": "2024-01-01T10:00:00Z",
  "ventanaHoras": 2
}
```

**Response:**
```json
{
  "pedidosPlanificados": 45,
  "pedidosRechazados": 3,
  "estadoPedidos": [
    {
      "idPedido": 1234,
      "idCadena": "000000001-20240101-10-38-EBCI-006-0007729",
      "estado": "PENDIENTE",
      "cumpleSla": true,
      "fechaEntregaEstimada": "2024-01-03T14:30:00Z",
      "cantidadTramos": 2
    }
  ],
  "mensaje": "Planificación completada: 45 pedidos planificados, 3 rechazados"
}
```

#### GET `/api/dia-a-dia/estado`
Consulta el estado actual del backlog

---

## 4. Reglas de Negocio Implementadas

✅ **Terminología**: Pedido/envío, entregado/recibido (sin usar "paquete")
✅ **Estructura de pedidos**: 1-999 productos por pedido, destino único
✅ **Formato ID**: `id_pedido-aaaammdd-hh-mm-dest-###-IdClien`
✅ **Almacenes hub**: Pedidos a hubs (Lima, Bruselas, Bakú) se excluyen
✅ **SLA**: 2 días mismo continente, 3 días continentes diferentes
✅ **Procesamiento destino**: +2 horas en aeropuerto destino
✅ **Reasignaciones**: Permitidas en almacenes de paso, no en destino final
✅ **Cancelaciones**: Solo en tierra, antes del despegue
✅ **Tiempos**: Estancia mínima 1 hora, carga/descarga instantánea

---

## 5. Parámetros ACO Configurables

En `AcoParameters.java`:

```java
private double alpha = 1.0;           // Importancia de la feromona
private double beta = 2.0;            // Importancia de la heurística
private double rho = 0.5;             // Tasa de evaporación
private double q0 = 0.9;              // Explotar vs explorar
private int numeroHormigas = 50;
private int numeroIteraciones = 100;
```

Puedes ajustar estos valores para optimizar el algoritmo.

---

## 6. Próximos Pasos

### Mejoras Prioritarias

1. **Implementar cálculo real de continentes** en `CalculadorSLA.java`:
   - Leer continente de productos desde BD
   - Comparar con continente de destino
   - Aplicar SLA correcto (2 o 3 días)

2. **Mejorar algoritmo ACO**:
   - Implementar búsqueda de rutas reales (multi-hop)
   - Agregar restricciones de capacidad en vuelos
   - Optimizar función heurística

3. **Validaciones adicionales**:
   - Verificar tiempo mínimo de estancia (1 hora)
   - Validar que productos no se reasignen en destino final
   - Implementar lógica de liberación de espacio después de entrega

4. **Testing**:
   - Tests unitarios para servicios ACO
   - Tests de integración con BD
   - Tests de rendimiento con grandes volúmenes

5. **Modo Colapso**:
   - Implementar tercer modo de simulación
   - Manejar cancelaciones masivas de vuelos

---

## 7. Troubleshooting

### Error: "Table doesn't exist"
- Asegúrate de ejecutar todos los scripts SQL en MySQL
- Verifica que las tablas T08-T12 existan en el schema `morapack2`

### Error de compilación
- Ejecuta `mvnw.cmd clean compile` desde la raíz del proyecto
- Verifica que Java 21 esté instalado: `java -version`

### Puerto 8080 ocupado
- Cambia el puerto en `application.properties`: `server.port=8081`

### Swagger no funciona
- Accede a `http://localhost:8080/swagger-ui/index.html`
- Verifica que SpringDoc esté en el pom.xml

---

## 8. Contacto y Soporte

Si encuentras problemas, revisa:
1. Logs del backend en consola
2. Tablas de MySQL (verificar datos insertados)
3. Swagger para probar endpoints manualmente

**Arquitectura diseñada para ser extensible y cumplir con todos los requerimientos del proyecto MoraPack.**
