$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $projectRoot 'backend')
& .\mvnw.cmd -B -ntp spring-boot:run
exit $LASTEXITCODE
