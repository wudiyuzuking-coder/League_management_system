. "$PSScriptRoot\env.ps1"

if (-not $LeagueTicketMysqlExe) {
    throw 'Set MYSQL_EXE to mysql.exe or add mysql.exe to PATH.'
}
if (-not $env:DB_USERNAME) {
    throw 'Set DB_USERNAME before stopping the project MySQL helper.'
}

$mysqladmin = Join-Path (Split-Path -Parent $LeagueTicketMysqlExe) 'mysqladmin.exe'
$mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { '127.0.0.1' }
$mysqlPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { '3306' }
$oldMysqlPassword = $env:MYSQL_PWD
if ($null -ne $env:DB_PASSWORD) { $env:MYSQL_PWD = $env:DB_PASSWORD }
& $mysqladmin --protocol=tcp --host=$mysqlHost --port=$mysqlPort --user=$env:DB_USERNAME shutdown
$env:MYSQL_PWD = $oldMysqlPassword
