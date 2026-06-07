@echo off
chcp 65001 >nul
cd /d "%~dp0"
data\jre\bin\java.exe -jar mcsm-backend-0.0.1-SNAPSHOT.jar
pause
