. "$PSScriptRoot\env.ps1"

if (-not $LeagueTicketMysqlExe) {
    throw 'Set MYSQL_EXE to mysql.exe or add mysql.exe to PATH.'
}
if (-not $env:DB_USERNAME) {
    throw 'Set DB_USERNAME before starting the project MySQL helper.'
}

$mysqlBin = Split-Path -Parent $LeagueTicketMysqlExe
$mysqlHome = Split-Path -Parent $mysqlBin
$mysqladmin = Join-Path $mysqlBin 'mysqladmin.exe'
$mysqld = Join-Path $mysqlBin 'mysqld.exe'
$mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { '127.0.0.1' }
$mysqlPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { '3306' }
$pidFile = Join-Path $LeagueTicketProjectRoot '.tools\mysql.pid'
$oldMysqlPassword = $env:MYSQL_PWD
if ($null -ne $env:DB_PASSWORD) { $env:MYSQL_PWD = $env:DB_PASSWORD }

& $mysqladmin --protocol=tcp --host=$mysqlHost --port=$mysqlPort --user=$env:DB_USERNAME ping *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "MySQL is already running on ${mysqlHost}:${mysqlPort}."
    $env:MYSQL_PWD = $oldMysqlPassword
    exit 0
}
if (-not $LeagueTicketMysqlData) {
    $env:MYSQL_PWD = $oldMysqlPassword
    throw 'MySQL is not running. Set MYSQL_DATA_DIR to use the local start helper.'
}
if (-not (Test-Path -LiteralPath $mysqld)) {
    $env:MYSQL_PWD = $oldMysqlPassword
    throw "mysqld.exe was not found beside MYSQL_EXE: $mysqld"
}

$args = @(
    "--basedir=$mysqlHome",
    "--datadir=$LeagueTicketMysqlData",
    "--port=$mysqlPort",
    "--bind-address=$mysqlHost",
    '--character-set-server=utf8mb4',
    '--collation-server=utf8mb4_0900_ai_ci',
    '--skip-log-bin'
)

$process = Start-Process -FilePath $mysqld -ArgumentList $args -WindowStyle Hidden -PassThru
$process.Id | Set-Content $pidFile
Start-Sleep -Seconds 5

& $mysqladmin --protocol=tcp --host=$mysqlHost --port=$mysqlPort --user=$env:DB_USERNAME ping
$env:MYSQL_PWD = $oldMysqlPassword
