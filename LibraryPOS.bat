@echo off
title Library POS System
echo Starting Library POS System...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo.
    echo Please install Java 17 or later from:
    echo https://www.java.com/download/
    echo.
    pause
    exit /b 1
)

REM Run the application
java -jar pos-system-0.0.1-SNAPSHOT.jar

REM If the application exits, pause so user can see any error messages
if errorlevel 1 (
    echo.
    echo Application exited with an error.
    pause
)
