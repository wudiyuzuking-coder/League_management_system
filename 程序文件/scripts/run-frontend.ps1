. "$PSScriptRoot\env.ps1"

Set-Location (Join-Path $LeagueTicketProjectRoot 'frontend')
npm run dev
