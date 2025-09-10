@echo off
title Building Microbot Mule Bridge
echo ================================
echo  BUILDING MULE BRIDGE JAR
echo ================================
echo.
echo Cleaning previous build...
mvn clean

echo.
echo Building JAR file (skipping tests)...
mvn package -DskipTests

echo.
rem Find built jar (Spring Boot default: mule-bridge-<version>.jar)
set "JAR="
for %%f in ("target\mule-bridge-*.jar") do (
    set "JAR=%%f"
)
if defined JAR (
    echo ================================
    echo ✓ SUCCESS! JAR file created:
    echo   Location: %JAR%
    echo   Size:
    dir "%JAR%" | find "mule-bridge"
    echo ================================
    echo.
    echo You can now start the bridge with:
    echo   start-bridge.bat
    echo   or
    echo   start-bridge-console.bat
    echo ================================
) else (
    echo ================================
    echo ✗ ERROR: JAR file not created!
    echo Check the build output above for errors
    echo (Tip: ensure Java 11+ and Maven are installed and on PATH)
    echo ================================
)

echo.
echo Press any key to close...
pause 
