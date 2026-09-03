$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = 'dev'
}

$MysqlExe = $env:MYSQL_EXE
if (-not $MysqlExe) {
    $MysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($MysqlCommand) {
        $MysqlExe = $MysqlCommand.Source
    }
}

if ($MysqlExe) {
    if (-not (Test-Path -LiteralPath $MysqlExe)) {
        throw "MYSQL_EXE does not exist: $MysqlExe"
    }
    $MysqlExe = (Resolve-Path -LiteralPath $MysqlExe).Path
    $MysqlBin = Split-Path -Parent $MysqlExe
    if (($env:PATH -split ';') -notcontains $MysqlBin) {
        $env:PATH = "$MysqlBin;$env:PATH"
    }
}

$global:LeagueTicketProjectRoot = $ProjectRoot
$global:LeagueTicketMysqlExe = $MysqlExe
$global:LeagueTicketMysqlData = $env:MYSQL_DATA_DIR
