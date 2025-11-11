# Cómo Iniciar el Backend - MoraPack

## ✅ El problema se solucionó

Había 3 errores que impedían que el servidor iniciara:

1. **Hibernate intentaba modificar tablas existentes** → Solución: Cambié `ddl-auto=update` a `ddl-auto=none`
2. **Error en repositorio**: Método `findBySeveridadAndResueltaFalse` (debía ser `Resuelto`) → Solucionado
3. **Error de CORS**: Configuración con `allowCredentials` → Agregué `allowCredentials="false"`

---

## 🚀 Cómo Iniciar el Servidor

### Paso 1: Compilar (si hiciste cambios)

```cmd
cd C:\Lhia\Cursos Catolica\DP1\Proyecto\MoraPack_Backend
mvnw.cmd clean compile
```

### Paso 2: Iniciar el Servidor

```cmd
mvnw.cmd spring-boot:run
```

**Espera a ver este mensaje:**
```
Started MorapackBackendApplication in X.XXX seconds
```

---

## ✅ Probar que Funciona

### Abre otra terminal y ejecuta:

```powershell
curl http://localhost:8080/api/simulacion/health
```

**Debe responder:**
```
Servicio de simulación activo
```

---

## 🎯 Probar la Simulación

```powershell
$body = @{
    fechaInicio = "2025-11-01T00:00:00Z"
    fechaFin = "2025-11-07T23:59:59Z"
    hubsInfinitos = @("SPIM", "EBCI", "UBBB")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/simulacion/semanal/iniciar" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

---

## 🛠️ Si Necesitas Detener el Servidor

Presiona `Ctrl + C` en la terminal donde está corriendo.

O desde otra terminal:
```powershell
Stop-Process -Name java -Force
```

---

## 📝 Archivos Modificados

Se corrigieron estos 3 archivos:

1. `src/main/resources/application.properties`
   - Cambio: `ddl-auto=update` → `ddl-auto=none`

2. `src/main/java/com/morapack/repository/T14AlertaNearCollapseRepository.java`
   - Cambio: `findBySeveridadAndResueltaFalse` → `findBySeveridadAndResueltoFalse`

3. `src/main/java/com/morapack/controller/SimulacionController.java`
   - Cambio: `@CrossOrigin(origins = "*")` → `@CrossOrigin(origins = "*", allowCredentials = "false")`

---

## ✅ Resumen

El backend ahora:
- ✅ Compila correctamente
- ✅ Inicia sin errores
- ✅ Responde en el puerto 8080
- ✅ Endpoints funcionan

**Todo listo para usar** 🎉
