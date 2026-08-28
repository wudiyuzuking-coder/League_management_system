. "$PSScriptRoot\env.ps1"

$mysqladmin = Join-Path $LeagueTicketMysqlHome 'bin\mysqladmin.exe'
$mysqld = Join-Path $LeagueTicketMysqlHome 'bin\mysqld.exe'
$pidFile = Join-Path $LeagueTicketProjectRoot '.tools\mysql.pid'

& $mysqladmin --protocol=tcp --host=127.0.0.1 --port=3315 --user=root ping *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host 'MySQL is already running on 127.0.0.1:3315.'
    exit 0
}

$args = @(
    "--basedir=$LeagueTicketMysqlHome",
    "--datadir=$LeagueTicketMysqlData",
    '--port=3315',
    '--bind-address=127.0.0.1',
    '--character-set-server=utf8mb4',
    '--collation-server=utf8mb4_0900_ai_ci',
    '--skip-log-bin'
)

$process = Start-Process -FilePath $mysqld -ArgumentList $args -WindowStyle Hidden -PassThru
$process.Id | Set-Content $pidFile
Start-Sleep -Seconds 5

& $mysqladmin --protocol=tcp --host=127.0.0.1 --port=3315 --user=root ping
