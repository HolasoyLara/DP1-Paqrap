# Días hasta el colapso (Algoritmo Genético)

Escenario *Simulación hasta el colapso* con el planificador GA, sin animación, datos publicados del curso y
parámetros por defecto: semilla 20260902, ventana de planificación de 60 min, averías aleatorias activadas
(0,6 por día) y colapso al haber **3 pedidos activos vencidos a la vez** (o 15 pedidos sin unidad que no pueden
esperar). Cada corrida se limita a 45 días simulados.

| Fecha de inicio | Resultado | Fecha del colapso | Pedidos registrados | Cumplimiento a tiempo |
|---|---|---|---|---|
| 01/11/2026 | **Sin colapso en 45 días** | — | 8 438 | 99,9 % |
| 01/01/2027 | Colapsa a los **37,5 días** | 07/02/2027 11:00 | 6 788 | 99,8 % |
| **01/03/2027** (fecha por defecto del escenario) | Colapsa a los **4,9 días** | 05/03/2027 21:00 | 1 440 | 99,7 % |
| 01/05/2027 | Colapsa a los **1,9 días** | 02/05/2027 21:00 | 713 | 99,2 % |
| 01/07/2027 | Colapsa a los **1,7 días** | 02/07/2027 16:00 | 725 | 99,0 % |
| 01/01/2028 | Colapsa a los **0,6 días** | 01/01/2028 14:00 | 375 | 98,9 % |

En todos los casos el motivo es el mismo: varios pedidos activos dejan de cumplir su plazo al mismo tiempo. El
colapso llega antes cuanto más tarde es la fecha de inicio, porque la demanda de los archivos de ventas crece con
el tiempo.

El cumplimiento alto en el momento del colapso se explica porque el criterio es estricto (3 pedidos vencidos a la
vez): el sistema se declara colapsado en cuanto deja de poder atender a tiempo, no cuando ya incumple a la mayoría.
Con un umbral mayor (Parámetros avanzados › *Pedidos incumplidos a la vez*) la operación dura más.

Para repetir la medición: ver [desarrollo.md](desarrollo.md#días-hasta-el-colapso).
