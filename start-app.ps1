$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$frontendRoot = Join-Path $projectRoot 'frontend'
$backendLog = Join-Path $projectRoot 'backend.log'
$frontendLog = Join-Path $projectRoot 'frontend.log'

function Wait-Port {
  param(
    [int]$Port,
    [int]$TimeoutSeconds = 60
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $tcp = New-Object Net.Sockets.TcpClient
      $iar = $tcp.BeginConnect('127.0.0.1', $Port, $null, $null)
      $connected = $iar.AsyncWaitHandle.WaitOne(300)
      if ($connected -and $tcp.Connected) {
        $tcp.Close()
        return $true
      }
      $tcp.Close()
    } catch {
    }
  }

  return $false
}

Write-Host '[1/4] Iniciando backend Java...'
Start-Process -FilePath 'powershell' -ArgumentList @(
  '-NoExit',
  '-ExecutionPolicy', 'Bypass',
  '-Command',
  "Set-Location '$projectRoot'; mvn spring-boot:run *>> '$backendLog'"
) | Out-Null

if (-not (Wait-Port -Port 8080 -TimeoutSeconds 90)) {
  throw 'Backend Java nao subiu na porta 8080. Verifique backend.log.'
}

Write-Host '[2/4] Iniciando frontend React...'
Start-Process -FilePath 'powershell' -ArgumentList @(
  '-NoExit',
  '-ExecutionPolicy', 'Bypass',
  '-Command',
  "Set-Location '$frontendRoot'; npm run dev -- --host 127.0.0.1 --port 5173 *>> '$frontendLog'"
) | Out-Null

if (-not (Wait-Port -Port 5173 -TimeoutSeconds 60)) {
  throw 'Frontend nao subiu na porta 5173. Verifique frontend.log.'
}

Write-Host '[3/4] Abrindo aplicacao no navegador...'
Start-Process 'http://127.0.0.1:5173'

Write-Host '[4/4] Aplicacao iniciada com sucesso.'
