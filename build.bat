@echo off
REM Simple batch script to build the SQL Spark Connector
REM This script checks for Java and Maven, then builds the project

setlocal enabledelayedexpansion

echo.
echo  ================================================
echo  SQL Spark Connector Build Script
echo  ================================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo.
    echo Please install Java 11 or higher from:
    echo   https://www.oracle.com/java/technologies/downloads/
    echo.
    echo After installation, make sure Java is in your PATH and close this window
    echo.
    pause
    exit /b 1
)

echo [OK] Java is installed:
java -version
echo.

REM Check if Maven is installed
mvn --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo.
    echo Please install Maven from:
    echo   https://maven.apache.org/download.cgi
    echo.
    echo After installation, make sure Maven is in your PATH and close this window
    echo.
    pause
    exit /b 1
)

echo [OK] Maven is installed:
mvn --version
echo.

REM Run the build
echo Building SQL Spark Connector with Spark 4.2.0...
echo.

mvn clean package -P spark42

if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ================================================
echo Build completed successfully!
echo ================================================
echo.
echo JAR files created in: target\
echo.
echo Main JAR: target\spark-mssql-connector-1.4.0.jar
echo.
echo You can now use this JAR in your Spark environment
echo.
pause
