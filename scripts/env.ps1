$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

$env:JAVA_HOME = Join-Path $ProjectRoot '.tools\jdk21-extract\jdk-21.0.12.1+1'
$NodeHome = Join-Path $ProjectRoot '.tools\node-extract\node-v24.19.0-win-x64'
$MysqlHome = 'C:\Users\Administrator\.codex\visualizations\2026\08\24\01a03363-2f7c-72e2-89d9-df937252cfc4\league-ticket-tools\mysql-8.0.44-winx64'
$MysqlData = 'C:\Users\Administrator\.codex\visualizations\2026\08\24\01a03363-2f7c-72e2-89d9-df937252cfc4\league-ticket-tools\mysql-data'

$env:PATH = "$env:JAVA_HOME\bin;$NodeHome;$MysqlHome\bin;$env:PATH"
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3315/league_ticket?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = ''
$env:JWT_SECRET = 'local-development-secret-local-development-secret'

$global:LeagueTicketProjectRoot = $ProjectRoot
$global:LeagueTicketMysqlHome = $MysqlHome
$global:LeagueTicketMysqlData = $MysqlData
