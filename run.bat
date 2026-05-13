@echo off
REM Usage: run.bat [server_ip] [port] [protocol]
REM Example: run.bat 192.168.1.50 6000 UDP
REM Default (single-player): run.bat
setlocal enabledelayedexpansion

if "%1"=="" (
    set SERVER_IP=localhost
    set PORT=6000
    set PROTOCOL=UDP
) else (
    set SERVER_IP=%1
    set PORT=%2
    set PROTOCOL=%3
)

java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 myGame.MyGame !SERVER_IP! !PORT! !PROTOCOL!