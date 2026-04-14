$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$startScript = Join-Path $projectRoot 'start-app.ps1'
$desktop = [Environment]::GetFolderPath('Desktop')
$shortcutPath = Join-Path $desktop 'Game Dashboard.lnk'

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = 'powershell.exe'
$shortcut.Arguments = "-ExecutionPolicy Bypass -File `"$startScript`""
$shortcut.WorkingDirectory = $projectRoot
$shortcut.IconLocation = 'shell32.dll,44'
$shortcut.Save()

Write-Host "Atalho criado em: $shortcutPath"
