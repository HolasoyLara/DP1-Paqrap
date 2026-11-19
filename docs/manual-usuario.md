# Manual de usuario

PaqRap planifica y muestra en un mapa las entregas de paquetes de una ciudad de 70 × 50 km con tres almacenes
(central, Nor-Oeste y Este) y una flota de autos, motos y bicicletas. Tiene tres escenarios:

| Escenario | Qué hace | Termina |
|---|---|---|
| **Simulación del periodo (5 días)** | Simula 5 días con los pedidos del archivo de ventas, acelerado para durar entre 30 y 60 min reales. | Al completar los 5 días o con «Detener corrida». |
| **Simulación hasta el colapso** | Simula sin límite, acelerado, hasta que la operación ya no da abasto. | Al detectar el colapso logístico o con «Detener corrida». |
| **Operación día a día** | Opera en tiempo real (1:1) desde la hora actual; admite pedidos manuales. | Con «Detener corrida». |

El planificador es el **Algoritmo Genético (GA)**. Con la misma fecha, los mismos archivos y la misma semilla,
la corrida sale siempre igual.

## Abrir la aplicación

- **En su PC (Windows):** `COMPILAR.bat` la primera vez, luego `INICIAR.bat`; se abre `http://localhost:8080`.
  `DETENER.bat` la apaga.
- **En la VM del laboratorio:** abra `http://IP-DE-LA-VM:8080` (ver [despliegue-vm.md](despliegue-vm.md)).

Varias personas pueden abrir la app a la vez, desde distintos dispositivos: todas ven **la misma corrida**,
pero cada una maneja su propio zoom, filtros y selección.

## Menú lateral

El botón **☰** (arriba a la izquierda) abre el menú:

- **Simulación** / **Operación día a día**: cambia de modo.
- **1 · Parámetros**: archivos, escenario y configuración de la corrida.
- **2 · Panorama (mapa)**: el mapa en vivo.
- **3 · Cierre**: reportes de la corrida y el historial.
- **4 · Pedidos**: registro y seguimiento de pedidos.

## 1. Parámetros

1. **Archivos de entrada.** Vienen cargados los datos publicados del curso (pedidos, bloqueos, mantenimiento y
   averías). Cada uno debe decir *Validado*. Puede reemplazarlos con **Subir**; si el periodo cruza de mes, suba
   2 archivos a la vez (uno por mes). El desplegable *Formatos de archivo* muestra el formato de cada uno.
2. **Configuración de la corrida.** Elija el escenario y la fecha/hora de inicio (hasta el minuto).
3. **Umbrales del semáforo.** Definen cuándo un pedido está a tiempo (verde), en riesgo (ámbar) o crítico
   (rojo), según el % de plazo que le queda. Se pueden cambiar incluso con la corrida en marcha.
4. **Parámetros avanzados** (opcional): ritmo de la simulación, criterios de colapso, averías aleatorias, flota
   (unidades, capacidad, velocidad y costo por tipo), plazos de entrega, tiempos de operación y parámetros del
   algoritmo. Con la corrida en marcha solo se pueden cambiar las velocidades por tipo de unidad
   (botón *Aplicar velocidades a la corrida en curso*).
5. Pulse **Iniciar corrida**.

## 2. Panorama (mapa)

### Barra de estado (arriba a la derecha)

- Estado de la corrida y **ocupación global** de la flota y de los almacenes, con su semáforo.
- **Los 4 tiempos:** fecha y hora simulada, tiempo simulado transcurrido, fecha y hora actual y tiempo real
  transcurrido (en *Operación día a día* no se muestran).
- **Ritmo** (solo 5 días y colapso): **❚❚ / ▶** pausa y reanuda; **−** y **+** cambian la velocidad entre ×¼ y ×8.
  ×1 es el ritmo base definido en Parámetros avanzados.
- **Información**: escenario, algoritmo, semilla, día, ritmo y el detalle de ocupación en paquetes.
- **Iniciar** / **Detener**.

### Controles del mapa (arriba a la izquierda)

| Botón | Qué hace |
|---|---|
| **Panel** | Listas de **Transporte**, **Almacenes** y **Envíos** (ver abajo). |
| **Métricas** | Cumplimiento a tiempo, pedidos entregados, costo, flota activa, stock de cada almacén y rutas recalculadas. |
| **Leyenda** | Qué significa cada ícono, color y línea del mapa. |
| **Filtros** | Zona visible (vista completa o un cuadrante), elementos visibles, tipo de unidad, semáforo de ocupación y qué rutas dibujar. *Restablecer filtros* vuelve a los valores iniciales. |
| **Buscar pedido** | Ubica un pedido por su código (p. ej. `PV2609-0001`) y abre su detalle. |
| **− / 100% / +** | Zoom. También con la rueda del mouse (centrado en el cursor) o pellizcando en pantallas táctiles; arrastre para desplazarse. |

### Qué se ve en el mapa

- **Almacenes:** el central (sin límite de stock) y los intermedios, con su stock y su semáforo.
- **Unidades en ruta:** auto, moto o bicicleta, coloreadas según su carga; las averiadas, en gris con una
  insignia 1, 2 o 3 según el tipo de avería.
- **Pedidos:** paquetes coloreados según el semáforo de plazo.
- **Rutas:** azul con guiones hacia los clientes; turquesa punteado de regreso al almacén.
- **Calles bloqueadas:** cinta amarilla con franjas negras, tanto las del archivo como las cerradas a mano.

### Acciones

- **Pasar el mouse** sobre una unidad, pedido o almacén: resumen rápido.
- **Clic** sobre una unidad, pedido o almacén: abre su detalle y lo resalta en el panel.
- **Clic en un tramo de calle:** lo bloquea o lo reabre; las unidades afectadas recalculan su ruta.
- **Operación día a día:** clic cerca de un cruce para registrar un pedido nuevo en ese punto.

### Panel de listas

- **Transporte:** cada unidad con su ocupación, estado, destino, hora de llegada y km. Filtros por ocupación,
  tipo, almacén de origen y distancia al destino; búsqueda por minutos o km al destino / desde el origen; orden
  por ocupación, hora de salida o de llegada, o km.
- **Almacenes:** stock y semáforo de cada almacén; pedidos en tránsito, por salir en las próximas horas y
  entregados en las últimas horas. Se puede ocultar un almacén (también en el mapa).
- **Envíos:** pedidos activos con destino, unidad, ruta y horarios; filtro de «a entregar en las próximas X h» y
  «entregados en las últimas X h» (X de 1 a 24).

Un clic en una fila ubica el elemento en el mapa.

### Detalle de una unidad o un pedido

- **Unidad:** ocupación, turno, velocidad y costo, próxima parada, mantenimiento, avería, la **ruta actual**
  tramo a tramo y las **rutas anteriores**. Con la corrida en marcha se puede **reportar una avería** (tipo 1, 2 o 3).
- **Pedido:** estado, plazo, holgura, unidades que lo llevan, **rutas anteriores** y su trazabilidad.
  En día a día, un pedido que aún no salió se puede editar o cancelar.
- **Ver en el mapa** en una ruta anterior la dibuja en magenta con sus escalas numeradas y sus horas;
  *Ocultar* la quita.

## 3. Cierre

Se genera al detener la corrida, al terminar los 5 días o al detectar el colapso: cumplimiento por día,
kilometraje y costo por tipo de unidad, utilización de la flota, incidencias, la última planificación y una
verificación automática de los plazos. **Exportar reporte** lo descarga en CSV para Excel. Debajo está el
**historial de corridas**.

## 4. Pedidos

- **Registrar pedido** (solo en *Operación día a día*): cliente, punto de entrega, paquetes y tipo de entrega.
  Los pedidos de más de 24 paquetes se entregan en partes.
- **Cargar pedidos en lote** desde un archivo con el formato de ventas (también solo en *Operación día a día*),
  y **Exportar pedidos** a CSV.
- **Pedidos de la corrida:** búsqueda por código o cliente, estado y tipo de entrega; un clic abre su
  trazabilidad completa.
