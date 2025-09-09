@echo off
color 0A
title Microbot Mule Bridge - Console Panel
mode con cols=80 lines=30

:start
cls
echo.
echo  Server URL: http://localhost:8080
echo  Status: STARTING...
echo.
echo.

echo [%time%] Starting Spring Boot application...
java -jar target\mule-bridge-1.0.0.jar

echo.
echo  Options:
echo  [R] Restart server
echo  [Q] Quit
echo.
set /p choice="Enter your choice: "

if /i "%choice%"=="R" goto start
if /i "%choice%"=="Q" exit

goto start
