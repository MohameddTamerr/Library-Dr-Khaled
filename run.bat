@echo off
echo ========================================
echo Library POS System Launcher
echo ========================================
echo.

REM Try to find Java 21 from VS Code extension
set "JAVA_HOME=C:\Users\user\.antigravity\extensions\redhat.java-1.51.0-win32-x64\jre\21.0.9-win32-x86_64"

if exist "%JAVA_HOME%\bin\java.exe" (
    echo Found Java 21 in VS Code extensions!
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    echo Java 21 not found in expected location.
    echo Checking system Java version...
    java -version 2>&1 | findstr /C:"21" >nul
    if %errorlevel%==1 (
        echo WARNING: System Java does not appear to be Java 21.
        echo Application might fail with "cannot find symbol" errors.
    )
)

echo Starting application...
echo.

mvnw.cmd spring-boot:run

pause
