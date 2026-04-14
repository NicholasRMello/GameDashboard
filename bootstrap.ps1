param(
  [switch]$AutoInstall
)

$ErrorActionPreference = 'Stop'

function Test-CommandExists {
  param([string]$Name)
  return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Assert-OrInstall {
  param(
    [string]$Command,
    [string]$DisplayName,
    [string]$WingetId
  )

  if (Test-CommandExists -Name $Command) {
    Write-Host "[ok] $DisplayName encontrado."
    return
  }

  if (-not $AutoInstall) {
    throw "$DisplayName nao encontrado. Rode novamente com -AutoInstall para instalar automaticamente."
  }

  if (-not (Test-CommandExists -Name 'winget')) {
    throw "winget nao encontrado para instalar $DisplayName automaticamente."
  }

  Write-Host "[setup] Instalando $DisplayName..."
  winget install --id $WingetId --accept-source-agreements --accept-package-agreements
}

Write-Host "[1/4] Verificando dependencias..."
Assert-OrInstall -Command 'java' -DisplayName 'Java (JDK 17+)' -WingetId 'EclipseAdoptium.Temurin.17.JDK'
Assert-OrInstall -Command 'mvn' -DisplayName 'Maven' -WingetId 'Apache.Maven'
Assert-OrInstall -Command 'node' -DisplayName 'Node.js' -WingetId 'OpenJS.NodeJS.LTS'

Write-Host "[2/4] Instalando dependencias do frontend..."
Push-Location "$PSScriptRoot\frontend"
npm install
Pop-Location

Write-Host "[3/4] Validando backend Java..."
Push-Location $PSScriptRoot
mvn -q test
Pop-Location

Write-Host "[4/4] Criando atalho na area de trabalho..."
& "$PSScriptRoot\create-desktop-shortcut.ps1"

Write-Host "Concluido. Execute o atalho 'Game Dashboard.lnk' na area de trabalho para iniciar tudo."
