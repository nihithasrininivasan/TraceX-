param([string]$TestAdminEmail, [switch]$RestartBackend, [switch]$ReadOnlyCheck, [switch]$FinalVerification)
# Collect the existing MySQL login locally. Never writes credentials to a file.
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$form = New-Object System.Windows.Forms.Form
$form.Text = 'TraceX - existing local MySQL login'
$form.ClientSize = New-Object System.Drawing.Size(420, 250)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'FixedDialog'
$form.MaximizeBox = $false
$form.MinimizeBox = $false
$form.TopMost = $true
$fields = @{}
$row = 18
foreach ($name in @('Username', 'Database', 'Password')) {
    $label = New-Object System.Windows.Forms.Label
    $label.Text = $name
    $label.Location = New-Object System.Drawing.Point(20, $row)
    $label.Size = New-Object System.Drawing.Size(100, 24)
    $form.Controls.Add($label)
    $inputBox = New-Object System.Windows.Forms.TextBox
    $inputBox.Location = New-Object System.Drawing.Point(125, $row)
    $inputBox.Size = New-Object System.Drawing.Size(270, 24)
    if ($name -eq 'Username') { $inputBox.Text = 'root' }
    if ($name -eq 'Database') { $inputBox.Text = 'traceroute_db' }
    if ($name -eq 'Password') { $inputBox.UseSystemPasswordChar = $true }
    $fields[$name] = $inputBox
    $form.Controls.Add($inputBox)
    $row += 43
}
$note = New-Object System.Windows.Forms.Label
$note.Text = 'Connects to MySQL on this PC (port 3306). Password is used in memory only.'
$note.Location = New-Object System.Drawing.Point(20, 150)
$note.Size = New-Object System.Drawing.Size(380, 38)
$form.Controls.Add($note)
$connect = New-Object System.Windows.Forms.Button
$connect.Text = 'Connect and start TraceX'
if ($ReadOnlyCheck) { $form.Text = 'TraceX - read-only database check'; $connect.Text = 'Check database'; $note.Text = 'Read-only checks on local MySQL. No records or services will be changed.' }
if ($FinalVerification) { $form.Text = 'TraceX - final verification'; $connect.Text = 'Run verification'; $note.Text = 'Reads your existing database, then tests APIs in a separate verification database. Existing accounts/reports stay unchanged.' }
$connect.Size = New-Object System.Drawing.Size(190, 30)
$connect.Location = New-Object System.Drawing.Point(205, 200)
$connect.DialogResult = [System.Windows.Forms.DialogResult]::OK
$form.Controls.Add($connect)
$form.AcceptButton = $connect
if ($form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { Write-Output 'MySQL login cancelled.'; exit 2 }
$dbName = $fields['Database'].Text.Trim()
$dbUser = $fields['Username'].Text.Trim()
if ($dbName -notmatch '^[a-zA-Z0-9_]+$' -or !$dbUser) { throw 'Enter a database name containing only letters, numbers and underscores, and a username.' }
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
try {
    $env:MYSQL_PWD = $fields['Password'].Text
    $fields['Password'].Clear()
    if ($FinalVerification) {
        $env:TRACEX_VERIFY_DB_USER=$dbUser
        $env:TRACEX_VERIFY_SOURCE_DB=$dbName
        & python (Join-Path $PSScriptRoot 'final_verification.py')
        exit $LASTEXITCODE
    }
    if ($ReadOnlyCheck) {
        $inspection = @'
START TRANSACTION READ ONLY;
SELECT DATABASE() AS database_name, VERSION() AS mysql_version;
SHOW TABLES;
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'incidents', COUNT(*) FROM incidents
UNION ALL SELECT 'incident_relationships', COUNT(*) FROM incident_relationships
UNION ALL SELECT 'evidence', COUNT(*) FROM evidence
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs;
SELECT status, COUNT(*) AS incident_count FROM incidents GROUP BY status;
SELECT incident_id, incident_type, status, domain, reported_at FROM incidents ORDER BY incident_id DESC LIMIT 10;
SELECT 'incidents_with_missing_user' AS integrity_check, COUNT(*) AS problem_count FROM incidents i LEFT JOIN users u ON i.user_id=u.id WHERE i.user_id IS NOT NULL AND u.id IS NULL
UNION ALL SELECT 'evidence_with_missing_incident', COUNT(*) FROM evidence e LEFT JOIN incidents i ON e.incident_id=i.incident_id WHERE i.incident_id IS NULL
UNION ALL SELECT 'relationships_with_missing_incident', COUNT(*) FROM incident_relationships r LEFT JOIN incidents a ON r.source_incident_id=a.incident_id LEFT JOIN incidents b ON r.target_incident_id=b.incident_id WHERE a.incident_id IS NULL OR b.incident_id IS NULL
UNION ALL SELECT 'invalid_relationship_score', COUNT(*) FROM incident_relationships WHERE matching_score < 0 OR matching_score > 1 OR matching_score IS NULL
UNION ALL SELECT 'empty_evidence_content', COUNT(*) FROM evidence WHERE content IS NULL OR OCTET_LENGTH(content)=0
UNION ALL SELECT 'unexpected_password_hash_format', COUNT(*) FROM users WHERE LEFT(password_hash,4) NOT IN ('$2a$','$2b$','$2y$') OR CHAR_LENGTH(password_hash)<>60;
SELECT source_incident_id, target_incident_id, relationship_type, matching_score FROM incident_relationships ORDER BY relationship_id DESC LIMIT 10;
COMMIT;
'@
        $inspection | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --port=3306 --user=$dbUser --database=$dbName --connect-timeout=5 --table
        if ($LASTEXITCODE -ne 0) { throw 'The read-only database check failed.' }
        exit 0
    }
    & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --port=3306 --user=$dbUser --connect-timeout=5 -e "CREATE DATABASE IF NOT EXISTS $dbName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; SELECT 1 AS connection_ok;"
    if ($LASTEXITCODE -ne 0) { throw 'MySQL connection failed. Verify the username, password and database privileges.' }
    if ($TestAdminEmail) {
        if ($TestAdminEmail -notmatch '^qa\.admin\.[a-f0-9-]+@campus\.test$') { throw 'Only a freshly registered QA administrator address is supported.' }
        & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --port=3306 --user=$dbUser --database=$dbName --batch --skip-column-names -e "UPDATE users SET role='ADMIN' WHERE email='$TestAdminEmail' AND role='STUDENT'; SELECT ROW_COUNT() AS test_admin_promoted; SELECT table_name FROM information_schema.tables WHERE table_schema='$dbName' ORDER BY table_name; SELECT COUNT(*) AS test_incidents FROM incidents WHERE suspicious_url LIKE 'https://fakebank.test/%'; SELECT COUNT(*) AS relationships FROM incident_relationships; SELECT COUNT(*) AS evidence_records FROM evidence; SELECT COUNT(*) AS audit_records FROM audit_logs;"
        if ($LASTEXITCODE -ne 0) { throw 'Test administrator provisioning failed.' }
        Write-Output 'Only the named QA account was eligible for promotion. Existing users were unchanged.'
        if (!$RestartBackend) { exit 0 }
    }
    $env:DB_PASSWORD = $env:MYSQL_PWD
    $env:DB_USER = $dbUser
    $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/$dbName"
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
    if ($RestartBackend) {
        $knownAppPids = @()
        Get-ChildItem -LiteralPath $runtime -Filter 'Backend*.out.log' | ForEach-Object {
            Get-Content -LiteralPath $_.FullName | ForEach-Object {
                if ($_ -match 'INFO\s+(\d+)\s+---.+TraceRouteApplication\s+: Starting') { $knownAppPids += [int]$Matches[1] }
            }
        }
        $listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        foreach ($listener in $listeners) {
            if ($listener.OwningProcess -notin $knownAppPids) { throw 'Port 8080 belongs to a process not identified by the TraceX startup logs; it was not stopped.' }
            Stop-Process -Id $listener.OwningProcess -ErrorAction Stop
        }
        Start-Sleep -Seconds 2
    }
    $shell = (Get-Process -Id $PID).Path
    $helper = Join-Path $PSScriptRoot 'Run-Backend.ps1'
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $process = Start-Process -FilePath $shell -ArgumentList @('-NoProfile', '-File', ('"' + $helper + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "Backend-$stamp.out.log") -RedirectStandardError (Join-Path $runtime "Backend-$stamp.err.log")
    Set-Content -LiteralPath (Join-Path $runtime 'Backend.pid') -Value $process.Id
    Write-Output "MySQL login succeeded. Backend launcher PID: $($process.Id)."
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    $form.Dispose()
}
