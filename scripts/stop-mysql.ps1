. "$PSScriptRoot\env.ps1"

$mysqladmin = Join-Path $LeagueTicketMysqlHome 'bin\mysqladmin.exe'
& $mysqladmin --protocol=tcp --host=127.0.0.1 --port=3315 --user=root shutdown
