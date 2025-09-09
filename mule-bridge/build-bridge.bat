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
if exist "target\MuleBridge.jar" (
    echo ================================
    echo ✓ SUCCESS! JAR file created:
    echo   Location: target\MuleBridge.jar
    echo   Size:
    dir target\MuleBridge.jar | find "MuleBridge.jar"
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
    echo ================================
)

echo.
echo Press any key to close...
pause >nul
