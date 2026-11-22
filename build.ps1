param([switch]$Test, [switch]$Portable, [string]$Jdk)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Set-Location -LiteralPath $PSScriptRoot
. (Join-Path $PSScriptRoot 'resolve-jdk.ps1')
$Jdk = Resolve-GameJdk $Jdk
Write-Host "使用 JDK：$Jdk"
$javac = Join-Path $Jdk 'bin\javac.exe'
$java = Join-Path $Jdk 'bin\java.exe'
$jar = Join-Path $Jdk 'bin\jar.exe'
New-Item -ItemType Directory -Path 'target\classes','target\test-classes' -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& $javac --release 21 -encoding UTF-8 -d 'target\classes' @sources
if ($LASTEXITCODE -ne 0) { throw 'Java 编译失败。' }
& $jar --create --file 'target\neon-last-light-2.0.0.jar' --main-class studio.lastlight.Main -C 'target\classes' .
if ($LASTEXITCODE -ne 0) { throw '游戏打包失败。' }
if ($Test) {
    $tests = @(Get-ChildItem -LiteralPath 'src\test\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
    & $javac --release 21 -encoding UTF-8 -cp 'target\classes' -d 'target\test-classes' @tests
    if ($LASTEXITCODE -ne 0) { throw '测试代码编译失败。' }
    & $java '-Djava.awt.headless=true' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'target\classes;target\test-classes' studio.lastlight.GameTests
    if ($LASTEXITCODE -ne 0) { throw '游戏验证失败。' }
}
if ($Portable) {
    # 每次生成独立目录，避免覆盖用户以前的便携包或存档。
    $destination = Join-Path 'dist' (Get-Date -Format 'yyyyMMdd-HHmmss')
    $staging = 'target\package-input'
    New-Item -ItemType Directory -Path $staging -Force | Out-Null
    & $java '-Djava.awt.headless=true' -cp 'target\classes' studio.lastlight.GameIcon 'target\neon-last-light.ico'
    if ($LASTEXITCODE -ne 0) { throw '图标生成失败。' }
    Copy-Item -LiteralPath 'target\neon-last-light-2.0.0.jar','LICENSE','README.md' -Destination $staging -Force
    Copy-Item -LiteralPath 'docs' -Destination $staging -Recurse -Force
    & (Join-Path $Jdk 'bin\jpackage.exe') --type app-image --name '霓虹最后的光' --input $staging --main-jar 'neon-last-light-2.0.0.jar' --main-class studio.lastlight.Main --icon 'target\neon-last-light.ico' --dest $destination --temp (Join-Path 'target' ('jpackage-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))) --app-version '2.0.0' --vendor '最后的光工作室' --description '离线单机生存射击游戏' --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw '便携版打包失败。' }
    Write-Host "便携游戏：$destination\霓虹最后的光\霓虹最后的光.exe"
}
Write-Host '构建成功：target\neon-last-light-2.0.0.jar'
