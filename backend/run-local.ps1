# Loads backend\.env into the current shell, then starts Spring Boot.
# Spring Boot does not read .env files natively — this script bridges that gap for local dev.

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "backend\.env not found. Copy backend\.env.example to backend\.env, fill Firebase credentials, and re-run."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    Set-Item -Path "Env:$name" -Value $value
}

& (Join-Path $PSScriptRoot "mvnw.cmd") spring-boot:run
