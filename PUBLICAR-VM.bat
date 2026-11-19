@echo off
rem PaqRap: publica la version compilada en una VM Linux por SSH.
rem Uso: PUBLICAR-VM.bat usuario@servidor [-Puerto 8080] [-Compilar]
setlocal
title PaqRap - Publicar en la VM
if "%~1"=="" (
  echo Uso: PUBLICAR-VM.bat usuario@servidor [-Puerto 8080] [-Compilar]
  pause
  exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows\publicar-vm.ps1" -Servidor %*
if errorlevel 1 (
  echo.
  echo Ocurrio un problema. Revise los mensajes anteriores.
  pause
  exit /b 1
)
pause
exit /b 0
