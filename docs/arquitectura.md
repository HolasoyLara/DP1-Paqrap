# Arquitectura y código

## Visión general

```
Navegador (Next.js / React)  ──HTTP JSON──▶  Spring Boot (Java 21)  ──JPA──▶  H2 (o PostgreSQL)
   visualizador estático                       API REST + motor de simulación
   (servido por el mismo backend)              + planificador (GA / ACO)
```

- **Un solo servicio.** El visualizador se compila como sitio estático y se empaqueta dentro del `.jar` del
  backend (`backend/src/main/resources/static`). Así se despliega un único archivo, `paqrap-app.jar`.
- **Una corrida a la vez, compartida.** El servidor ejecuta una sola corrida; todos los navegadores consultan su
  estado. El zoom, los filtros y la selección son locales a cada navegador; los comandos (bloquear una calle,
  reportar una avería, registrar un pedido, cambiar umbrales, velocidades o ritmo) afectan a todos.
- **Simulación por eventos.** El motor procesa en orden de tiempo los eventos de la corrida (llegada de pedidos,
  ventanas de planificación, bloqueos, averías, mantenimientos, recarga de almacenes, entregas) y no paso a paso
  por minuto. Ver [diagrama-consumo-datos-Sc-Sa.svg](diagrama-consumo-datos-Sc-Sa.svg).

## Backend (`backend/src/main/java/pe/pucp/paqrap`)

| Paquete | Responsabilidad | Clases principales |
|---|---|---|
| `api` | Controladores REST, manejo de errores y exportación CSV. | `SimulacionController`, `PedidosController`, `DatosController`, `CorridasController`, `ExperimentosController`, `ConfiguracionController` |
| `simulacion` | Ciclo de vida de la corrida, motor de eventos, reloj y datos de entrada. | `ServicioSimulacion`, `Corrida`, `MotorSimulacion`, `FuenteDatos`, `ParametrosCorrida`, `VehiculoSim`, `RutaEnCurso`, `ServicioExperimentacion` |
| `planificador` | Algoritmos de ruteo, red vial y modelo del problema. | `Planificador`, `AlgoritmoGenetico`, `AlgoritmoColoniaHormigas`, `ModeloRuteo`, `RedVial`, `EntregasParciales` |
| `flota` | Vehículos, almacenes, turnos, averías, bloqueos y mantenimiento, y la lectura de sus archivos. | `Vehiculo` (`Auto`, `Moto`, `Bicicleta`), `AlmacenCentral`, `AlmacenIntermedio`, `TipoAveria`, `JornadaConductor`, `PlanMantenimiento` |
| `registro` | Pedidos, clientes y su trazabilidad; lectura del archivo de ventas. | `Pedido`, `SeguimientoPedido`, `LectorVentas`, `TipoEntrega` |
| `persistencia` | Entidades JPA: corridas, eventos, rutas e historial de experimentos. | `ServicioPersistencia`, `CorridaEntidad`, `RutaHistorialEntidad` |
| `visualizador` | Lo que se envía al navegador: estado del mapa y detalles. | `EstadoVisual`, `DetalleVisual` |
| `comun` | Tiempo, coordenadas, parámetros de operación, umbrales, estadística. | `ConversorTiempo`, `ParametrosOperacion`, `UmbralesSemaforo` |

### Ciclo de una corrida

1. `POST /api/simulacion/iniciar` → `ServicioSimulacion.iniciar` normaliza los `ParametrosCorrida`, carga los
   datos del periodo con `FuenteDatos` y crea un `MotorSimulacion` y una `Corrida`.
2. `Corrida` corre en su propio hilo: cada **100 ms reales** avanza el reloj simulado
   (tiempo real × factor del escenario × velocidad elegida) con `motor.avanzarHasta(hora)` y cada **250 ms**
   publica un `EstadoVisual`. En pausa, el reloj no avanza.
3. `MotorSimulacion` procesa los eventos hasta esa hora. En cada **ventana de planificación** (60 min simulados
   por defecto) arma un `ModeloRuteo` con los pedidos pendientes y las unidades libres y llama al `Planificador`;
   también replanifica de inmediato ante un pedido manual, una avería o el inicio de un mantenimiento.
4. Cada ruta planificada se convierte en una `RutaEnCurso` con su **trayectoria** (puntos con hora de paso). Si
   la ruta cambia en camino (bloqueo, avería, reasignación), se cierra y la unidad sigue con una ruta de
   continuación; la cerrada queda en el historial de la unidad (rutas anteriores).
5. La corrida termina al cumplir los días del periodo, al detectar el colapso o con `POST /api/simulacion/detener`.
   Se genera el `CierreCorrida` (reportes) y se guarda en la base de datos.

### Escenarios

| Escenario | Factor del reloj | Fin |
|---|---|---|
| `SIMULACION_5D` | 120 h ÷ duración objetivo (45 min → 160×) | 5 días simulados |
| `COLAPSO_LOGISTICO` | configurable (160× por defecto) | colapso: 3 pedidos vencidos a la vez, o 15 pedidos sin unidad que no pueden esperar |
| `TIEMPO_REAL` | 1× | manual |

El mismo motor resuelve los tres: solo cambian el reloj, el origen de los pedidos y la condición de cierre.

### Planificador

`Planificador` aplica el patrón Strategy sobre `AlgoritmoMetaheuristico`:

- **`AlgoritmoGenetico` (en uso):** asignación inicial por inserción más barata y búsqueda genética sobre el
  orden de visita (cruce OX, mutación swap, torneo, elitismo).
- **`AlgoritmoColoniaHormigas`:** segundo algoritmo, usado en la experimentación numérica.

Ambos respetan capacidad, plazos, stock de cada almacén, turnos y alimentación, mantenimiento y bloqueos. Cada
ventana usa una semilla derivada de la semilla de la corrida, así las corridas son reproducibles.

`RedVial` calcula distancias en la retícula: Manhattan sin bloqueos; con bloqueos, BFS esquivando los nodos
bloqueados, con caché por tramo horario.

### Datos de entrada

- Publicados del curso dentro del `.jar` (`backend/src/main/resources/datos`): ventas y bloqueos de 2026-01 a
  2028-12, y el plan de mantenimiento.
- Se leen **por mes, bajo demanda**: el mes siguiente se carga 72 h antes de necesitarlo.
- La carpeta `datos-externos` (variable `PAQRAP_DATOS_EXTERNOS`) permite agregar o reemplazar meses sin
  recompilar, y en *Parámetros* se pueden subir archivos propios.

## API REST

Todas las rutas empiezan con `/api`. Errores: `{"error": "mensaje"}` con 400 (dato inválido), 404 (no existe)
o 409 (estado no válido, p. ej. no hay corrida en curso).

| Método y ruta | Uso |
|---|---|
| `GET /salud` · `GET /config` | Estado del servicio · valores por defecto (flota, almacenes, turnos, escenarios, algoritmos). |
| `GET /datos/archivos?escenario&fechaInicio&dias` | Validación de los archivos de entrada del periodo. |
| `POST /datos/archivos/{tipo}` · `DELETE /datos/archivos/{tipo}` | Subir o quitar un archivo propio (`ventas`, `bloqueos`, `mantenimiento`, `averias`). |
| `POST /simulacion/iniciar` · `POST /simulacion/detener` | Iniciar (cuerpo: `ParametrosCorrida`) o detener la corrida. |
| `GET /simulacion/estado` | Estado del mapa (`EstadoVisual`), compartido por todos los navegadores. |
| `GET /simulacion/vehiculos/{id}` · `pedidos/{id}` · `almacenes/{id}` | Detalle de una unidad, pedido o almacén. |
| `POST /simulacion/bloqueos` | Cerrar o reabrir un tramo `{x1, y1, x2, y2}`. |
| `POST /simulacion/averias` | Reportar una avería `{vehiculo, tipo, causa}`. |
| `PUT /simulacion/umbrales` · `velocidades` · `ritmo` | Cambiar umbrales del semáforo, velocidades por tipo o el ritmo `{velocidad, pausada}`. |
| `GET /simulacion/cierre` · `cierre/csv` | Cierre de la corrida (o vista previa) y su CSV. |
| `GET/POST/PUT/DELETE /pedidos...` · `POST /pedidos/lote` · `GET /pedidos/exportar` | Búsqueda, registro, edición, cancelación, carga masiva y exportación de pedidos. |
| `GET /corridas` · `/{id}` · `/{id}/csv` · `/{id}/rutas` · `/{id}/eventos` | Historial de corridas guardadas. |
| `POST /experimentos` · `GET /experimentos[/{id}[/csv]]` | Experimentación numérica GA vs ACO (ver [desarrollo.md](desarrollo.md)). |

## Frontend (`frontend/`)

Next.js 15 + React 19 en TypeScript, sin librerías de componentes; el mapa es un SVG.

| Archivo | Contenido |
|---|---|
| `app/page.tsx` | Página única: estado global, navegación entre pantallas y acciones (iniciar, detener, ritmo…). |
| `lib/useEstadoSimulacion.ts` | Consulta `GET /simulacion/estado` cada 1 s y extrapola el reloj entre consultas con el factor publicado. |
| `lib/api.ts` · `lib/tipos.ts` · `lib/formato.ts` | Cliente de la API, tipos de los datos y formatos (fechas, dinero, semáforo). |
| `components/MapaOperacion.tsx` | Mapa SVG: retícula, almacenes, unidades animadas sobre su trayectoria, rutas, bloqueos, zoom y filtros. |
| `components/PantallaPanorama.tsx` | Pantalla del mapa con sus paneles (filtros, leyenda, métricas, búsqueda). |
| `components/PanelListas.tsx` | Listas de Transporte, Almacenes y Envíos vinculadas al mapa. |
| `components/BarraMapa.tsx` · `TopBar.tsx` · `ControlRitmo.tsx` | Barra del mapa, cabecera de las otras pantallas y controles de ritmo. |
| `components/PantallaParametros.tsx` · `PantallaCierre.tsx` · `PantallaPedidos.tsx` | Las otras tres pantallas. |
| `components/ModalDetalle.tsx` | Detalle de unidad, pedido o almacén (incluye rutas anteriores). |

Las unidades se animan sin pedir su posición a cada instante: el servidor envía la trayectoria restante de cada
unidad (`[x, y, minuto, …]`) y el navegador interpola la posición con el reloj extrapolado.
