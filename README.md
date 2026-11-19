# PaqRap

Planificación y visualización de entregas de PaqRap: un planificador (Algoritmo Genético) y un visualizador web
para los tres escenarios del curso: operación día a día, simulación del periodo de 5 días y simulación hasta el
colapso logístico.

## Uso rápido (Windows)

1. `COMPILAR.bat`: compila el visualizador y el backend y deja `dist\paqrap-app.jar`.
2. `INICIAR.bat`: inicia el servicio y abre http://localhost:8080.
3. `DETENER.bat`: detiene el servicio.

Requisitos: PowerShell 5.1 o superior. Si faltan Java 21 o Node.js 20, `COMPILAR.bat` descarga versiones
portables la primera vez (necesita internet). La base de datos, las bitácoras y la carpeta de compilación quedan
en `%LOCALAPPDATA%\PaqRap` (o en la carpeta de la variable `PAQRAP_LOCAL`).

## Despliegue en la VM del laboratorio

```powershell
.\PUBLICAR-VM.bat usuario@ip-de-la-vm
```

Copia el `.jar` a la VM Linux y lo instala como servicio con Java 21. Detalle, configuración y operación en
[docs/despliegue-vm.md](docs/despliegue-vm.md).

## Documentación

| Documento | Para quién |
|---|---|
| [Manual de usuario](docs/manual-usuario.md) | Cómo usar la aplicación: parámetros, mapa, paneles, cierre y pedidos. |
| [Arquitectura y código](docs/arquitectura.md) | Cómo está hecho: componentes, ciclo de una corrida, planificador, API REST y frontend. |
| [Guía de desarrollo](docs/desarrollo.md) | Compilar, probar, desarrollar el frontend y repetir la experimentación. |
| [Despliegue en una VM Linux](docs/despliegue-vm.md) | Instalar, actualizar y operar el servicio. |
| [Días hasta el colapso](docs/resultados-colapso.md) | Resultados del escenario de colapso con el GA. |
| [Consumo de datos](docs/diagrama-consumo-datos-Sc-Sa.svg) · [Varios navegadores](docs/diagrama-varios-navegadores.svg) | Diagramas. |

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | Spring Boot 3.5 (Java 21): motor de simulación, planificador, API REST y datos del curso (`src/main/resources/datos`). |
| `frontend/` | Visualizador en Next.js 15 + React 19 (TypeScript). Se exporta estático dentro del backend. |
| `scripts/windows/` | Compilación, inicio, detención y publicación en la VM (los usan los `.bat`). |
| `scripts/linux/` | Instalación del servicio en la VM Linux. |
| `docs/` | Documentación y diagramas. |

## Configuración

El backend se configura con variables de entorno (ver `backend/src/main/resources/application.yml`), por ejemplo
`PORT`, `PAQRAP_DATOS_DIR`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
y `PAQRAP_CORS_ORIGENES`. El visualizador en modo desarrollo usa `BACKEND_URL` (ver `frontend/.env.example`).
No se versionan credenciales.

## Pruebas

```powershell
cd backend
.\mvnw.cmd test
```
