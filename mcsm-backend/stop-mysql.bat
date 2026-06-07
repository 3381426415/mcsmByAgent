@echo off
chcp 65001 >nul
cd /d "%~dp0"
data\jre\bin\java.exe -cp "data" StopCli
pause
