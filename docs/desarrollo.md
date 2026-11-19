# Guía de desarrollo

## Requisitos

- Windows con PowerShell 5.1 o superior (los scripts de `scripts/windows`).
- Java 21 o superior y Node.js 20 o superior. Si faltan, `COMPILAR.bat` descarga versiones portables.

## Compilar y ejecutar

| Acción | Comando |
|---|---|
| Compilar todo (visualizador + backend → `dist\paqrap-app.jar`) | `COMPILAR.bat` |
| Compilar y correr las pruebas | `powershell -ExecutionPolicy Bypass -File scripts\windows\compilar.ps1 -ConPruebas` |
| Iniciar (puerto 8080 o el siguiente libre) | `INICIAR.bat` |
| Detener | `DETENER.bat` |
| Publicar en la VM | `PUBLICAR-VM.bat usuario@servidor` (ver [despliegue-vm.md](despliegue-vm.md)) |

Los scripts trabajan en `%LOCALAPPDATA%\PaqRap` (compilación, caché de npm y Maven, base de datos y bitácora).
Para usar otra carpeta, por ejemplo si el disco C: tiene poco espacio, defina `PAQRAP_LOCAL` antes:

```powershell
$env:PAQRAP_LOCAL = 'E:\PaqRap-local'
.\COMPILAR.bat
```

La compilación regenera `backend/src/main/resources/static` (el visualizador empaquetado); esos cambios se
versionan junto con el código del frontend.

### Desarrollo del frontend con recarga en caliente

Con el backend iniciado (`INICIAR.bat`):

```powershell
cd frontend
npm install
npm run dev        # http://localhost:3000, reenvía /api al backend (BACKEND_URL, por defecto http://localhost:8080)
```

### Pruebas del backend

```powershell
cd backend
.\mvnw.cmd test
```

| Prueba | Qué verifica |
|---|---|
| `ApiIntegracionTest` | Flujo completo por la API: configuración, archivos, corrida día a día con pedido manual, bloqueo, avería, cierre y control de ritmo. |
| `ReglasCursoTest` | Reglas del curso: averías, jornada y alimentación, mantenimiento, formatos de archivo. |
| `MotorSimulacionTest` | Corridas de 5 días con GA y ACO, cambio de velocidades en caliente. |
| `ReasignacionCargaTest` | Reasignación de carga en tránsito al cliente más crítico. |
| `ExploracionColapsoTest` | Exploración (omitida por defecto): fecha de colapso según la fecha de inicio. |

## Experimentación

### Días hasta el colapso

```powershell
cd backend
.\mvnw.cmd test "-Dtest=ExploracionColapsoTest" "-Dpaqrap.explorar=true" "-Dpaqrap.algoritmos=GA"
```

Corre el escenario de colapso sin animación desde varias fechas de inicio (hasta 45 días cada una) e imprime la
fecha, los días transcurridos y el motivo. Resultados actuales: [resultados-colapso.md](resultados-colapso.md).

### Comparación GA vs ACO

`POST /api/experimentos` corre el mismo escenario con ambos algoritmos en varias instancias y calcula
estadísticas (media, desviación, prueba t). Cuerpo de ejemplo:

```json
{
  "escenario": "SIMULACION_5D",
  "fechaInicio": "2026-09-01T00:00:00",
  "instancias": 10,
  "diasEntreInstancias": 3,
  "algoritmos": ["GA", "ACO"]
}
```

`GET /api/experimentos/{id}` devuelve el avance y los resultados; `/csv` los exporta.

## Convenciones

- Código, nombres y textos en español.
- Comentarios solo donde explican una regla del negocio o una decisión que el código no deja clara.
- Los parámetros de una corrida viajan en `ParametrosCorrida` (y `ParametrosOperacion`): un parámetro nuevo se
  agrega allí, con su valor por defecto y su rango en `normalizar()`.
- Cualquier cambio visible del estado del mapa pasa por `EstadoVisual` (backend) y `lib/tipos.ts` (frontend).
