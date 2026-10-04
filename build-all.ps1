param(
    [string[]]$Targets = @('1.21.1','1.21.11','26.2','26.3'),
    [string]$Java21 = $env:JAVA21_HOME,
    [string]$Java25 = $env:JAVA25_HOME,
    [string]$Python = 'python',
    [string]$GradleCache = $env:GRADLE_USER_HOME,
    [string]$Gradle21,
    [string]$Gradle25,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
foreach ($target in $Targets) {
    if ($target -notin @('1.21.1','1.21.11','26.2','26.3')) { throw "Unsupported target: $target" }
}
function Confirm-Jdk([string]$Directory,[int]$Major) {
    if (!$Directory) { throw "Set JAVA${Major}_HOME or pass -Java${Major}." }
    $compiler = Join-Path $Directory 'bin/javac'
    if ($env:OS -eq 'Windows_NT') { $compiler += '.exe' }
    if (!(Test-Path -LiteralPath $compiler)) { throw "JDK not found: $Directory" }
    $actual = & $compiler --version
    if ($LASTEXITCODE -or $actual -notmatch "^javac $Major\b") { throw "Expected JDK $Major, got $actual" }
}
if (!$Gradle21) { $Gradle21 = Join-Path $PSScriptRoot $(if ($env:OS -eq 'Windows_NT') {'gradlew.bat'} else {'gradlew'}) }
if (!$Gradle25) { $Gradle25 = Join-Path $PSScriptRoot $(if ($env:OS -eq 'Windows_NT') {'ports/gradlew.bat'} else {'ports/gradlew'}) }
$priorJava = $env:JAVA_HOME
$priorCache = $env:GRADLE_USER_HOME
try {
    if ($GradleCache) { $env:GRADLE_USER_HOME = $GradleCache }
    & $Python -X utf8 tools/audit_resources.py --check
    if ($LASTEXITCODE) { throw 'Resource audit failed.' }
    $options = @('--no-daemon','--console=plain')
    if ($Offline) { $options += '--offline' }
    if ('1.21.1' -in $Targets) {
        Confirm-Jdk $Java21 21
        $env:JAVA_HOME = $Java21
        & $Gradle21 @options ':build'
        if ($LASTEXITCODE) { throw 'Minecraft 1.21.1 build failed.' }
    }
    $modern = @($Targets | Where-Object { $_ -ne '1.21.1' })
    if ($modern.Count) {
        Confirm-Jdk $Java25 25
        $env:JAVA_HOME = $Java25
        $tasks = @($modern | ForEach-Object { ":${_}:build" })
        & $Gradle25 @options -p ports "-Ptarget=$($modern -join ',')" "-PportPython=$Python" @tasks
        if ($LASTEXITCODE) { throw 'Modern Minecraft build failed.' }
    }
    & $Python -X utf8 tools/package_multiversion.py --targets ($Targets -join ',')
    if ($LASTEXITCODE) { throw 'Release verification failed.' }
} finally {
    $env:JAVA_HOME = $priorJava
    $env:GRADLE_USER_HOME = $priorCache
}
