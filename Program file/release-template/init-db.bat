@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "MYSQL_EXE="
for /f "delims=" %%I in ('where mysql.exe 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%I"
if not defined MYSQL_EXE (
    echo [ERROR] mysql.exe was not found.
    echo Install the MySQL 8 command-line client and add its bin directory to PATH.
    pause
    exit /b 1
)

set "SCHEMA_SQL=%~dp0database\schema.sql"
set "SEED_SQL=%~dp0database\seed.sql"
if not exist "%SCHEMA_SQL%" (
    echo [ERROR] Missing SQL file: "%SCHEMA_SQL%"
    pause
    exit /b 1
)
if not exist "%SEED_SQL%" (
    echo [ERROR] Missing SQL file: "%SEED_SQL%"
    pause
    exit /b 1
)

set "DB_HOST_INPUT=localhost"
set "DB_PORT_INPUT=3306"
set "DB_USER=root"
set /p "DB_HOST_INPUT=MySQL host [localhost]: "
if not defined DB_HOST_INPUT set "DB_HOST_INPUT=localhost"
set /p "DB_PORT_INPUT=MySQL port [3306]: "
if not defined DB_PORT_INPUT set "DB_PORT_INPUT=3306"
set /p "DB_USER=MySQL account [root]: "
if not defined DB_USER set "DB_USER=root"

echo.
echo The MySQL client will securely prompt for the password.
echo Initializing the non-destructive schema...
"%MYSQL_EXE%" --protocol=tcp --host="%DB_HOST_INPUT%" --port="%DB_PORT_INPUT%" --default-character-set=utf8mb4 --user="%DB_USER%" -p < "%SCHEMA_SQL%"
if errorlevel 1 (
    echo [ERROR] schema.sql failed. seed.sql was not executed.
    pause
    exit /b 1
)

echo Applying repeatable base data...
"%MYSQL_EXE%" --protocol=tcp --host="%DB_HOST_INPUT%" --port="%DB_PORT_INPUT%" --default-character-set=utf8mb4 --user="%DB_USER%" -p < "%SEED_SQL%"
if errorlevel 1 (
    echo [ERROR] seed.sql failed. Review the MySQL error above.
    pause
    exit /b 1
)

echo.
echo Database initialization completed successfully.
echo database\demo-data.sql was NOT imported; it is optional demo data and is destructive to existing business records.
echo Run database\test-data.sql manually for read-only seed verification.
pause
exit /b 0
