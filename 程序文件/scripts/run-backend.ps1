. "$PSScriptRoot\env.ps1"
& "$PSScriptRoot\start-mysql.ps1"

$env:DEMO_PASSWORD_INIT_ENABLED = 'true'
$env:DEMO_PASSWORD = '123456'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$JavaUnixDomainTmp = Join-Path $LeagueTicketProjectRoot '.tmp\java-uds'
if (-not (Test-Path $JavaUnixDomainTmp)) {
    New-Item -ItemType Directory -Path $JavaUnixDomainTmp | Out-Null
}
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$JavaUnixDomainTmp -Djava.net.preferIPv4Stack=true"

Set-Location (Join-Path $LeagueTicketProjectRoot 'backend')
.\mvnw.cmd spring-boot:run
