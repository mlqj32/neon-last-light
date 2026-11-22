param([string]$Jdk)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Set-Location -LiteralPath $PSScriptRoot
. (Join-Path $PSScriptRoot 'resolve-jdk.ps1')
$Jdk = Resolve-GameJdk $Jdk
Write-Host "使用 JDK：$Jdk"
if (-not (Test-Path -LiteralPath 'target\neon-last-light-2.0.0.jar')) { & "$PSScriptRoot\build.ps1" -Jdk $Jdk }
& (Join-Path $Jdk 'bin\java.exe') '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar 'target\neon-last-light-2.0.0.jar'
