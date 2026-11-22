# 本游戏需要 Java 21，目录中同时有多个版本时必须先检查版本。
function Resolve-GameJdk([string]$RequestedJdk) {
    if ($RequestedJdk) {
        $candidates = @($RequestedJdk)
    } else {
        $candidates = @($env:JAVA_HOME, 'D:\DevTools\ci-jdk\jdk-21.0.12.1+1') +
            @(Get-ChildItem -LiteralPath 'D:\DevTools\ci-jdk' -Directory -ErrorAction SilentlyContinue |
                ForEach-Object { $_.FullName })
    }
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        if (-not (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) -or
            -not (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe'))) { continue }
        $release = Join-Path $candidate 'release'
        if (-not (Test-Path -LiteralPath $release)) { continue }
        $versionLine = Get-Content -LiteralPath $release | Where-Object { $_ -match '^JAVA_VERSION="' } | Select-Object -First 1
        if ($versionLine -match '^JAVA_VERSION="(\d+)\.' -and [int]$Matches[1] -ge 21) {
            return (Get-Item -LiteralPath $candidate).FullName
        }
    }
    if ($RequestedJdk) { throw "指定目录不包含可用的 JDK 21 或更高版本：$RequestedJdk" }
    throw '未找到 JDK 21 或更高版本。请使用 -Jdk 参数指定 D 盘的 JDK 目录。'
}
