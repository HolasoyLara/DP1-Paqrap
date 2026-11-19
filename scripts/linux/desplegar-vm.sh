#!/usr/bin/env bash
# PaqRap - instala o actualiza el sistema en una VM Linux como servicio systemd.
#
# Uso (en la VM, con el .jar ya copiado):
#   sudo bash desplegar-vm.sh paqrap-app.jar [puerto]
#
# Primera vez: instala Java 21 si falta, crea el usuario de servicio "paqrap", las carpetas
# /opt/paqrap (programa), /var/lib/paqrap (base de datos H2) y /var/log/paqrap (bitacora),
# el archivo de configuracion /etc/paqrap/paqrap.env y el servicio paqrap.service.
# Siguientes veces: reemplaza el .jar (guarda el anterior como paqrap-app.jar.anterior)
# y reinicia el servicio. La base de datos y la configuracion se conservan.
set -euo pipefail

JAR_ORIGEN="${1:-}"
PUERTO="${2:-8080}"
DIR_APP=/opt/paqrap
DIR_DATOS=/var/lib/paqrap
DIR_LOGS=/var/log/paqrap
DIR_CONF=/etc/paqrap
SERVICIO=paqrap

info() { printf '\033[36m==> %s\033[0m\n' "$*"; }
ok() { printf '\033[32m%s\033[0m\n' "$*"; }
error() { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || error "Ejecute con sudo: sudo bash $0 paqrap-app.jar [puerto]"
[ -n "$JAR_ORIGEN" ] && [ -f "$JAR_ORIGEN" ] || error "No se encontro el archivo .jar. Uso: sudo bash $0 paqrap-app.jar [puerto]"
[[ "$PUERTO" =~ ^[0-9]+$ ]] || error "Puerto invalido: $PUERTO"

version_java() {
  local v=0
  if command -v java >/dev/null 2>&1; then
    v="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1)"
  fi
  echo "${v:-0}"
}

# 1) Java 21 o superior
if [ "$(version_java)" -lt 21 ]; then
  info "Instalando Java 21..."
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -qq
    apt-get install -y -qq openjdk-21-jre-headless curl
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y -q java-21-openjdk-headless curl
  elif command -v yum >/dev/null 2>&1; then
    yum install -y -q java-21-openjdk-headless curl
  else
    error "No se reconoce el gestor de paquetes. Instale Java 21 manualmente y vuelva a ejecutar."
  fi
fi
JAVA_BIN="$(readlink -f "$(command -v java)")"
info "Java: $JAVA_BIN ($(version_java))"
command -v curl >/dev/null 2>&1 || error "Falta curl (se usa para comprobar que el servicio responde)."

# 2) Usuario y carpetas
id "$SERVICIO" >/dev/null 2>&1 || useradd --system --home-dir "$DIR_APP" --shell /usr/sbin/nologin "$SERVICIO"
mkdir -p "$DIR_APP" "$DIR_DATOS" "$DIR_DATOS/datos-externos" "$DIR_LOGS" "$DIR_CONF"
chown -R "$SERVICIO:$SERVICIO" "$DIR_APP" "$DIR_DATOS" "$DIR_LOGS"

# 3) Programa (se guarda la version anterior)
if [ -f "$DIR_APP/paqrap-app.jar" ]; then
  info "Guardando la version anterior en $DIR_APP/paqrap-app.jar.anterior"
  cp -f "$DIR_APP/paqrap-app.jar" "$DIR_APP/paqrap-app.jar.anterior"
fi
install -m 0644 -o "$SERVICIO" -g "$SERVICIO" "$JAR_ORIGEN" "$DIR_APP/paqrap-app.jar"

# 4) Configuracion (solo se crea la primera vez, para no pisar cambios manuales)
if [ ! -f "$DIR_CONF/paqrap.env" ]; then
  info "Creando $DIR_CONF/paqrap.env"
  cat > "$DIR_CONF/paqrap.env" <<EOF
# Configuracion de PaqRap. Despues de editar: sudo systemctl restart $SERVICIO
PORT=$PUERTO
PAQRAP_DATOS_DIR=$DIR_DATOS
PAQRAP_DATOS_EXTERNOS=$DIR_DATOS/datos-externos
LOGGING_FILE_NAME=$DIR_LOGS/paqrap.log
TZ=America/Lima
JAVA_OPTS=-Xmx1g -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Duser.timezone=America/Lima
# Origenes permitidos para llamar a la API desde otro dominio (* = todos)
PAQRAP_CORS_ORIGENES=*
EOF
else
  info "Se conserva la configuracion existente ($DIR_CONF/paqrap.env)"
  PUERTO="$(sed -nE 's/^PORT=([0-9]+).*/\1/p' "$DIR_CONF/paqrap.env" | head -1)"
  PUERTO="${PUERTO:-8080}"
fi

# 5) Servicio systemd
cat > "/etc/systemd/system/$SERVICIO.service" <<EOF
[Unit]
Description=PaqRap - planificacion y visualizacion de entregas
After=network-online.target
Wants=network-online.target

[Service]
User=$SERVICIO
Group=$SERVICIO
WorkingDirectory=$DIR_APP
EnvironmentFile=$DIR_CONF/paqrap.env
ExecStart=/bin/sh -c 'exec $JAVA_BIN \$JAVA_OPTS -jar $DIR_APP/paqrap-app.jar'
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=30

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable "$SERVICIO" >/dev/null 2>&1
info "Reiniciando el servicio $SERVICIO..."
systemctl restart "$SERVICIO"

# 6) Firewall (solo si ufw o firewalld estan activos)
if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q "Status: active"; then
  ufw allow "$PUERTO/tcp" >/dev/null
elif command -v firewall-cmd >/dev/null 2>&1 && firewall-cmd --state >/dev/null 2>&1; then
  firewall-cmd --permanent --add-port="$PUERTO/tcp" >/dev/null && firewall-cmd --reload >/dev/null
fi

# 7) Espera a que responda /api/salud (hasta 3 minutos)
info "Esperando a que PaqRap responda en el puerto $PUERTO..."
for _ in $(seq 1 90); do
  if curl -fsS "http://localhost:$PUERTO/api/salud" >/dev/null 2>&1; then
    IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
    ok "PaqRap esta listo: http://${IP:-localhost}:$PUERTO"
    echo "    Estado:    sudo systemctl status $SERVICIO"
    echo "    Bitacora:  sudo journalctl -u $SERVICIO -f   (o $DIR_LOGS/paqrap.log)"
    echo "    Volver a la version anterior: sudo bash $0 $DIR_APP/paqrap-app.jar.anterior"
    exit 0
  fi
  if ! systemctl is-active --quiet "$SERVICIO"; then
    journalctl -u "$SERVICIO" -n 30 --no-pager || true
    error "El servicio se detuvo al iniciar. Revise los mensajes anteriores."
  fi
  sleep 2
done
journalctl -u "$SERVICIO" -n 30 --no-pager || true
error "PaqRap no respondio a tiempo. Revise: sudo journalctl -u $SERVICIO -n 100"
