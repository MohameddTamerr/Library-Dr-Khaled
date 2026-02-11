@echo off
echo ===========================================
echo   LibraryPOS - Create EXE (No Java Required)
echo ===========================================
echo.

REM 1. Clean and Build the JAR
echo [1/3] Building the application with Maven...
call .\mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Maven build failed!
    exit /b %ERRORLEVEL%
)

REM 2. Define Variables
set LOG_FILE=build_jpackage.log
set APP_VERSION=1.0.0
set MAIN_JAR=pos-system-0.0.1-SNAPSHOT.jar
set INPUT_DIR=target
set OUTPUT_DIR=dist
set APP_NAME=LibraryPOS
set MAIN_CLASS=com.library.pos.LibraryPosApplicationLauncher

REM 3. Run jpackage
echo.
echo [2/3] Generating executable with jpackage...
echo (This might take a minute...)

if exist "%OUTPUT_DIR%" (
    rmdir /s /q "%OUTPUT_DIR%"
)

jpackage ^
  --name "%APP_NAME%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --type app-image ^
  --dest "%OUTPUT_DIR%" ^
  --java-options "-Xmx512m" 

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] jpackage failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [3/4] Cleaning up and Hiding files...
cd "%OUTPUT_DIR%\%APP_NAME%\app"
if exist "classes" rmdir /s /q "classes"
if exist "maven-archiver" rmdir /s /q "maven-archiver"
if exist "maven-status" rmdir /s /q "maven-status"
if exist "generated-sources" rmdir /s /q "generated-sources"
if exist "generated-test-sources" rmdir /s /q "generated-test-sources"
if exist ".jpackage.xml" del /q ".jpackage.xml"
cd ..\..
REM Go back to project root

echo Hiding 'app' and 'runtime' folders...
attrib +h "%OUTPUT_DIR%\%APP_NAME%\app"
attrib +h "%OUTPUT_DIR%\%APP_NAME%\runtime"

echo.
echo [4/4] DONE!
echo.
echo The executable is located at:
echo %CD%\%OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
echo.
echo NOTE: The 'app' and 'runtime' folders are now HIDDEN.
echo You only see the .exe and any other files you add (like README).
echo.
echo You can zip the folder:
echo %CD%\%OUTPUT_DIR%\%APP_NAME%
echo and send it to your customer. They do NOT need Java installed.
echo.
pause
