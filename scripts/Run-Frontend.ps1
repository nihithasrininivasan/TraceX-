$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $projectRoot 'frontend')
$env:VITE_API_MODE = 'live'
& npm.cmd run dev
exit $LASTEXITCODE
