param(
    [string]$DatabaseUser,
    [string]$DatabaseName = 'traceroute_db',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 3306,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$JavaHome) {
    $candidate = Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Where-Object Name -Like 'jdk-21*' | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName }
}
if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Provide -JavaHome pointing to JDK 21.' }
if ($DatabaseName -notmatch '^[a-zA-Z0-9_]+$') { throw 'Database name must contain only letters, numbers and underscores.' }
if (!$DatabaseUser) { $DatabaseUser = Read-Host 'MySQL username' }
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (!(Test-Path -LiteralPath $mysql)) { $mysql = (Get-Command mysql.exe -ErrorAction Stop).Source }
$secret = Read-Host 'MySQL password (not displayed or saved)' -AsSecureString
$runtime = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
try {
    $env:MYSQL_PWD = [System.Net.NetworkCredential]::new('', $secret).Password
    # Additive only: never drops, truncates or overwrites a database.
    & $mysql --host=$DatabaseHost --port=$DatabasePort --user=$DatabaseUser --connect-timeout=5 -e "CREATE DATABASE IF NOT EXISTS $DatabaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) { throw 'MySQL login/database creation failed. Check the login and CREATE privilege.' }
    $env:DB_PASSWORD = $env:MYSQL_PWD
    $env:DB_USER = $DatabaseUser
    $env:DB_URL = "jdbc:mysql://${DatabaseHost}:${DatabasePort}/${DatabaseName}"
    $env:JAVA_HOME = $JavaHome
    $shell = (Get-Process -Id $PID).Path
    foreach ($part in @('Backend', 'Frontend')) {
        $helper = Join-Path $PSScriptRoot "Run-$part.ps1"
        $process = Start-Process -FilePath $shell -ArgumentList @('-NoProfile', '-File', ('"' + $helper + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "$part.out.log") -RedirectStandardError (Join-Path $runtime "$part.err.log")
        Set-Content -LiteralPath (Join-Path $runtime "$part.pid") -Value $process.Id
    }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}
$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    try {
        $health = Invoke-RestMethod 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
        $page = Invoke-WebRequest 'http://localhost:5173/' -UseBasicParsing -TimeoutSec 2
        if ($health.database -eq 'UP' -and $page.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Seconds 1 }
}
if (!$ready) { throw 'Servers did not become ready. Inspect .runtime logs. No password is printed by this script.' }
Write-Output 'Frontend: http://localhost:5173/'
Write-Output 'Backend health (with database query): http://127.0.0.1:8080/api/health'
