@echo off
set APP_VERSION=1.0.0
set MAIN_JAR=pos-system-0.0.1-SNAPSHOT.jar
set INPUT_DIR=target
set OUTPUT_DIR=dist
set APP_NAME=LibraryPOS
set MAIN_CLASS=com.library.pos.LibraryPosApplicationLauncher

echo [2/3] Generating executable with jpackage...
if exist "%OUTPUT_DIR%" (
    rmdir /s /q "%OUTPUT_DIR%"
)

"C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\jpackage.exe" ^
  --name "%APP_NAME%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --type app-image ^
  --dest "%OUTPUT_DIR%" ^
  --java-options "-Xmx512m" 

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] jpackage failed!
    exit /b %ERRORLEVEL%
)

echo [3/4] Cleaning up and Hiding files...
cd "%OUTPUT_DIR%\%APP_NAME%\app"
if exist "classes" rmdir /s /q "classes"
if exist "maven-archiver" rmdir /s /q "maven-archiver"
if exist "maven-status" rmdir /s /q "maven-status"
if exist "generated-sources" rmdir /s /q "generated-sources"
if exist "generated-test-sources" rmdir /s /q "generated-test-sources"
if exist ".jpackage.xml" del /q ".jpackage.xml"
cd ..\..

echo Hiding 'app' and 'runtime' folders...
attrib +h "%OUTPUT_DIR%\%APP_NAME%\app"
attrib +h "%OUTPUT_DIR%\%APP_NAME%\runtime"

echoDONE
