# Despliegue en una VM Linux

PaqRap se despliega como **un solo servicio**: el archivo `paqrap-app.jar` contiene el backend (API) y el
visualizador web. En la VM solo hace falta **Java 21**; el script lo instala si falta.

Al terminar, la aplicación queda en `http://IP-DE-LA-VM:8080`, arranca sola al reiniciar la VM y guarda su
base de datos en `/var/lib/paqrap`.

## Requisitos

| Dónde | Qué |
|---|---|
| VM | Linux con `systemd` (Ubuntu/Debian, o RHEL/Rocky/Fedora), acceso por SSH y un usuario con `sudo`. |
| VM | Internet la primera vez (para instalar Java 21), o Java 21 ya instalado. |
| Red | El puerto elegido (8080 por defecto) abierto desde los navegadores que van a usar la app. |
| PC con Windows | El repositorio, y el **Cliente OpenSSH** de Windows (viene en Windows 10/11; si falta: Configuración › Aplicaciones › Características opcionales). |

## Opción A: desde Windows, con un solo comando (recomendada)

1. Compile la versión que va a publicar (si no lo hizo antes):

   ```powershell
   .\COMPILAR.bat
   ```

2. Publique en la VM (reemplace usuario e IP):

   ```powershell
   .\PUBLICAR-VM.bat alumno@10.0.0.25
   ```

   El script copia `dist\paqrap-app.jar` y `scripts/linux/desplegar-vm.sh` a la VM por `scp`, y ejecuta la
   instalación con `sudo` (le pedirá la contraseña de SSH y la de `sudo`). Opciones:

   - `-Puerto 9090`: otro puerto (solo se usa la primera vez; luego se cambia en la configuración).
   - `-Compilar`: compila antes de publicar.

3. Abra `http://10.0.0.25:8080` en el navegador.

Para **actualizar** a una versión nueva, repita los pasos 1 y 2: el script reemplaza el `.jar`, guarda el
anterior como respaldo y reinicia el servicio. La base de datos y la configuración se conservan.

## Opción B: manual, desde la propia VM

```bash
# 1) Copiar el .jar y el script a la VM (desde el PC)
scp dist/paqrap-app.jar scripts/linux/desplegar-vm.sh alumno@10.0.0.25:/tmp/

# 2) En la VM
ssh alumno@10.0.0.25
sudo bash /tmp/desplegar-vm.sh /tmp/paqrap-app.jar 8080
```

Si el script se copió desde Windows y falla con `$'\r': command not found`, quite los fines de línea de
Windows con `sed -i 's/\r$//' /tmp/desplegar-vm.sh` y vuelva a ejecutarlo.

## Qué instala el script

| Elemento | Ubicación |
|---|---|
| Programa | `/opt/paqrap/paqrap-app.jar` (y `paqrap-app.jar.anterior`, la versión previa) |
| Base de datos H2 | `/var/lib/paqrap/` |
| Archivos de datos adicionales (opcional) | `/var/lib/paqrap/datos-externos/` |
| Bitácora | `/var/log/paqrap/paqrap.log` y `journalctl -u paqrap` |
| Configuración | `/etc/paqrap/paqrap.env` |
| Servicio | `paqrap.service` (usuario de sistema `paqrap`, se inicia con la VM) |

Si `ufw` o `firewalld` están activos, el script abre el puerto. Si la VM está detrás de otro firewall (por
ejemplo, el de la red del laboratorio o un *security group* en la nube), hay que abrirlo allí también.

## Configuración

Edite `/etc/paqrap/paqrap.env` y reinicie el servicio:

```bash
sudo nano /etc/paqrap/paqrap.env
sudo systemctl restart paqrap
```

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `PORT` | `8080` | Puerto HTTP. |
| `PAQRAP_DATOS_DIR` | `/var/lib/paqrap` | Carpeta de la base de datos H2. |
| `PAQRAP_DATOS_EXTERNOS` | `/var/lib/paqrap/datos-externos` | Archivos `ventas.AAAAMM.txt` / `bloqueo.AAMM.txt` que reemplazan o amplían los incluidos en el `.jar`. |
| `JAVA_OPTS` | `-Xmx1g ...` | Memoria y opciones de Java. Con muchos días de colapso se puede subir a `-Xmx2g`. |
| `PAQRAP_CORS_ORIGENES` | `*` | Orígenes que pueden llamar a la API desde otro dominio. |
| `SPRING_DATASOURCE_URL` (opcional) | H2 en archivo | Para usar PostgreSQL: `jdbc:postgresql://host:5432/paqrap` (más `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`). |

## Operación diaria

```bash
sudo systemctl status paqrap        # estado
sudo systemctl restart paqrap       # reiniciar
sudo systemctl stop paqrap          # detener
sudo journalctl -u paqrap -f        # bitácora en vivo
curl http://localhost:8080/api/salud   # debe responder {"estado":"OK",...}
```

**Volver a la versión anterior:**

```bash
sudo bash /tmp/desplegar-vm.sh /opt/paqrap/paqrap-app.jar.anterior
```

**Empezar con la base de datos vacía** (borra el historial de corridas):

```bash
sudo systemctl stop paqrap
sudo rm -rf /var/lib/paqrap/paqrap.*
sudo systemctl start paqrap
```

## Problemas frecuentes

| Síntoma | Causa probable y solución |
|---|---|
| El navegador no abre la página | El puerto está cerrado en el firewall de la red o de la VM. Desde la VM, `curl http://localhost:8080/api/salud` confirma que el servicio funciona. |
| `El servicio se detuvo al iniciar` | Revise `sudo journalctl -u paqrap -n 100`. Lo más común: el puerto ya está en uso (cambie `PORT`) o falta memoria (baje `-Xmx`). |
| `No se reconoce el gestor de paquetes` | Instale Java 21 manualmente (`java -version` debe decir 21 o más) y repita el despliegue. |
| La hora simulada o las fechas salen corridas | El servicio usa la zona `America/Lima` (`TZ` y `-Duser.timezone` en `paqrap.env`); no la quite. |

## Alternativa con Docker

El repositorio también incluye `docker-compose.yml` y los `Dockerfile` del backend y del frontend, por si la
VM tiene Docker: `docker compose up -d --build` publica la app en el puerto 8080.
