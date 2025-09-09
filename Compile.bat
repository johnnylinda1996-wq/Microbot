@echo off
REM Zorg dat MAVEN_HOME en Java in PATH staan

echo Cleaning project...
mvn clean compile -e

echo.
echo Build complete. Controleer bovenstaande errors.
pause