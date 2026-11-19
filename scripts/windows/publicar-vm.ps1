# PaqRap - publica la version compilada en una VM Linux por SSH.
# Copia dist\paqrap-app.jar y scripts\linux\desplegar-vm.sh a la VM y ejecuta la instalacion
# (o actualizacion) del servicio. Usa el cliente OpenSSH que ya trae Windows 10/11.
# Uso: PUBLICAR-VM.bat usuario@servidor [-Puerto 8080] [-Compilar]
#  o:  powershell -ExecutionPolicy Bypass -File scripts\windows\publicar-vm.ps1 -Servidor usuario@servidor
param(
    [Parameter(Mandatory = $true)][string]$Servidor,
    [int]$Puerto = 8080,
    [switch]$Compilar
)

. (Join-Path $PSScriptRoot 'comun.ps1')

try {
    foreach ($exe in 'ssh.exe', 'scp.exe') {
        if (-not (Get-Command $exe -ErrorAction SilentlyContinue)) {
            throw "No se encontro $exe. Active el 'Cliente OpenSSH' en Configuracion > Aplicaciones > Caracteristicas opcionales."
        }
    }
    if ($Compilar -or -not (Test-Path -LiteralPath $script:Jar)) {
        & (Join-Path $PSScriptRoot 'compilar.ps1')
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $script:Jar)) { throw 'La compilacion no genero dist\paqrap-app.jar.' }
    }
    $script = Join-Path $script:Raiz 'scripts\linux\desplegar-vm.sh'
    $mb = [math]::Round((Get-Item -LiteralPath $script:Jar).Length / 1MB, 1)

    Escribir-Paso "Copiando paqrap-app.jar ($mb MB) y desplegar-vm.sh a ${Servidor}:/tmp ..."
    & scp.exe $script:Jar $script "${Servidor}:/tmp/"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar a la VM (revise el usuario, la IP y el acceso SSH).' }

    Escribir-Paso 'Instalando o actualizando el servicio en la VM (pedira la contrasena de sudo)...'
    & ssh.exe -t $Servidor "sed -i 's/\r$//' /tmp/desplegar-vm.sh && sudo bash /tmp/desplegar-vm.sh /tmp/paqrap-app.jar $Puerto"
    if ($LASTEXITCODE -ne 0) { throw 'La instalacion en la VM no termino bien. Revise los mensajes anteriores.' }
    Escribir-Ok 'Publicacion terminada.'
    exit 0
} catch {
    Escribir-Error "No se pudo publicar PaqRap: $($_.Exception.Message)"
    exit 1
}
