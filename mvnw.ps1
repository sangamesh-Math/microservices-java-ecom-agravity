# Maven Wrapper PowerShell script
[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$ErrorActionPreference = "Stop"

$ProjectDir = $PSScriptRoot
$DotMvnDir = Join-Path $ProjectDir ".mvn"
$WrapperDir = Join-Path $DotMvnDir "wrapper"
$PropertiesFile = Join-Path $WrapperDir "maven-wrapper.properties"

$DistributionUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"

if (Test-Path $PropertiesFile) {
    Get-Content $PropertiesFile | ForEach-Object {
        if ($_ -match "^\s*distributionUrl\s*=\s*(.+)$") {
            $DistributionUrl = $matches[1].Trim()
        }
    }
}

$M2Home = Join-Path ([Environment]::GetFolderPath("UserProfile")) ".m2"
$DistsDir = Join-Path $M2Home "wrapper\dists"

# Compute hash/dir name for distribution
$ZipFileName = Split-Path $DistributionUrl -Leaf
$DistName = [System.IO.Path]::GetFileNameWithoutExtension($ZipFileName) -replace "-bin", ""
$DistDir = Join-Path $DistsDir $DistName

$MvnExecutable = Join-Path $DistDir "$DistName\bin\mvn.cmd"
if (-not (Test-Path $MvnExecutable)) {
    # Check if nested folder has bin/mvn.cmd
    $found = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $MvnExecutable = $found.FullName
    }
}

if (-not (Test-Path $MvnExecutable)) {
    Write-Host "Maven not found in cache. Downloading from $DistributionUrl..."
    if (-not (Test-Path $DistDir)) {
        New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
    }

    $ZipFilePath = Join-Path $DistDir $ZipFileName
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13
    Invoke-WebRequest -Uri $DistributionUrl -OutFile $ZipFilePath -UseBasicParsing

    Write-Host "Unpacking $ZipFileName..."
    Expand-Archive -Path $ZipFilePath -DestinationPath $DistDir -Force
    Remove-Item $ZipFilePath -Force -ErrorAction SilentlyContinue

    $found = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $MvnExecutable = $found.FullName
    } else {
        throw "Could not find mvn.cmd after unpacking Maven"
    }
}

Write-Host "Using Maven: $MvnExecutable"
& $MvnExecutable $Arguments
exit $LASTEXITCODE
