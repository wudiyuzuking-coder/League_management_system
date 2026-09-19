@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist "config.bat" (
    echo [ERROR] config.bat was not found.
    echo Copy config.bat.example to config.bat, then edit the local values.
    pause
    exit /b 1
)

call "config.bat"
if errorlevel 1 (
    echo [ERROR] config.bat could not be loaded.
    pause
    exit /b 1
)

for %%V in (SPRING_PROFILES_ACTIVE DB_URL DB_USERNAME JWT_SECRET APP_UPLOAD_DIR) do (
    if not defined %%V (
        echo [ERROR] %%V is not configured in config.bat.
        pause
        exit /b 1
    )
)

if "%JWT_SECRET%"=="replace-with-at-least-32-random-bytes" (
    echo [ERROR] Replace the example JWT_SECRET in config.bat before starting.
    pause
    exit /b 1
)

set "JAR_PATH=%~dp0league-ticket.jar"
if not exist "%JAR_PATH%" (
    echo [ERROR] Missing executable JAR: "%JAR_PATH%"
    pause
    exit /b 1
)

set "JAVA_EXE="
for /f "delims=" %%I in ('where java.exe 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%I"
if not defined JAVA_EXE (
    echo [ERROR] Java was not found. Install Java 17 or newer and add java.exe to PATH.
    pause
    exit /b 1
)

if not exist "%APP_UPLOAD_DIR%" mkdir "%APP_UPLOAD_DIR%"
if errorlevel 1 (
    echo [ERROR] Cannot create upload directory: "%APP_UPLOAD_DIR%"
    pause
    exit /b 1
)

echo Starting League Ticket at http://localhost:8080 ...
echo Keep this window open while using the system. Press Ctrl+C to stop it.
start "" /B powershell.exe -NoProfile -WindowStyle Hidden -Command "$deadline=(Get-Date).AddSeconds(60); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/health' -TimeoutSec 2; if ($r.StatusCode -eq 200) { Start-Process 'http://localhost:8080'; break } } catch {}; Start-Sleep -Milliseconds 750 } while ((Get-Date) -lt $deadline)"

"%JAVA_EXE%" -jar "%JAR_PATH%"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if not "%EXIT_CODE%"=="0" echo [ERROR] League Ticket stopped with exit code %EXIT_CODE%. Review the messages above.
if "%EXIT_CODE%"=="0" echo League Ticket has stopped.
pause
exit /b %EXIT_CODE%
