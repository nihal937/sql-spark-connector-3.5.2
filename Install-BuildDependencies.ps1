# PowerShell script to install Java 11 and Maven for SQL Spark Connector build
# Run this script as Administrator: Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

param(
    [switch]$SkipJava,
    [switch]$SkipMaven
)

function Write-Header {
    param([string]$Message)
    Write-Host "`n==================================" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "==================================" -ForegroundColor Cyan
}

function Test-InPath {
    param([string]$Command)
    $null = Get-Command $Command -ErrorAction SilentlyContinue
    return $?
}

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "Warning: This script should be run as Administrator for best results." -ForegroundColor Yellow
    Write-Host "Some installations may be blocked without admin privileges." -ForegroundColor Yellow
    Write-Host ""
}

Write-Header "SQL Spark Connector Build Environment Setup"

# Check current installations
Write-Host "Checking current installations..." -ForegroundColor Yellow

$javaInstalled = Test-InPath "java"
$mvnInstalled = Test-InPath "mvn"

Write-Host "Java installed: $(if ($javaInstalled) { 'Yes' } else { 'No' })" -ForegroundColor $(if ($javaInstalled) { 'Green' } else { 'Red' })
Write-Host "Maven installed: $(if ($mvnInstalled) { 'Yes' } else { 'No' })" -ForegroundColor $(if ($mvnInstalled) { 'Green' } else { 'Red' })

# If both installed, offer to skip
if ($javaInstalled -and $mvnInstalled) {
    Write-Host "`nBoth Java and Maven are already installed!" -ForegroundColor Green
    java -version
    Write-Host ""
    mvn --version
    Write-Host "`nBuild environment is ready. You can skip installation steps." -ForegroundColor Green
    exit 0
}

# Install Java
if (-not $SkipJava -and -not $javaInstalled) {
    Write-Header "Installing Java 11"
    
    Write-Host "Attempting to install Oracle JDK 11..." -ForegroundColor Yellow
    Write-Host "Note: This requires your consent for Microsoft Store terms." -ForegroundColor Yellow
    
    $jdkOptions = @(
        "Oracle.JDK.11",
        "Microsoft.OpenJDK.11",
        "EclipseAdoptium.Temurin.11"
    )
    
    $installed = $false
    foreach ($jdk in $jdkOptions) {
        try {
            Write-Host "Trying to install $jdk..." -ForegroundColor Gray
            & winget install -e --accept-source-agreements $jdk
            if ($LASTEXITCODE -eq 0) {
                $installed = $true
                Write-Host "Successfully installed $jdk" -ForegroundColor Green
                break
            }
        } catch {
            Write-Host "Failed to install $jdk" -ForegroundColor Yellow
        }
    }
    
    if (-not $installed) {
        Write-Host "Could not install Java via winget. Please install manually:" -ForegroundColor Yellow
        Write-Host "1. Download from: https://www.oracle.com/java/technologies/downloads/#java11" -ForegroundColor Cyan
        Write-Host "2. Run installer and complete installation" -ForegroundColor Cyan
        Write-Host "3. Close and reopen this PowerShell window" -ForegroundColor Cyan
        Write-Host "4. Run this script again" -ForegroundColor Cyan
        $javaInstalled = $false
    } else {
        # Refresh Environment
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        $javaInstalled = Test-InPath "java"
    }
}

# Install Maven
if (-not $SkipMaven -and -not $mvnInstalled) {
    Write-Header "Installing Maven"
    
    Write-Host "Attempting to install Apache Maven..." -ForegroundColor Yellow
    
    try {
        & winget install -e --accept-source-agreements Apache.Maven
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Successfully installed Apache Maven" -ForegroundColor Green
            # Refresh Environment
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            $mvnInstalled = Test-InPath "mvn"
        } else {
            Write-Host "Failed to install Maven via winget" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Error installing Maven: $_" -ForegroundColor Yellow
    }
    
    if (-not $mvnInstalled) {
        Write-Host "Could not install Maven via winget. Please install manually:" -ForegroundColor Yellow
        Write-Host "1. Download from: https://maven.apache.org/download.cgi" -ForegroundColor Cyan
        Write-Host "2. Extract to: C:\tools\apache-maven-3.9.x" -ForegroundColor Cyan
        Write-Host "3. Set MAVEN_HOME environment variable to that path" -ForegroundColor Cyan
        Write-Host "4. Add %MAVEN_HOME%\bin to your PATH" -ForegroundColor Cyan
        Write-Host "5. Close and reopen this PowerShell window" -ForegroundColor Cyan
        Write-Host "6. Run this script again" -ForegroundColor Cyan
    }
}

# Verify installations
Write-Header "Verifying Installation"

$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

$javaInstalled = Test-InPath "java"
$mvnInstalled = Test-InPath "mvn"

if ($javaInstalled) {
    Write-Host "Java:" -ForegroundColor Green
    java -version 2>&1 | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "Java: NOT FOUND" -ForegroundColor Red
}

Write-Host ""

if ($mvnInstalled) {
    Write-Host "Maven:" -ForegroundColor Green
    mvn --version 2>&1 | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "Maven: NOT FOUND" -ForegroundColor Red
}

# Build instructions
Write-Header "Next Steps"

if ($javaInstalled -and $mvnInstalled) {
    Write-Host "[SUCCESS] Build environment is ready!" -ForegroundColor Green
    Write-Host ""
    Write-Host "To build the project, run:" -ForegroundColor Cyan
    Write-Host "  cd '$PWD'" -ForegroundColor Gray
    Write-Host "  mvn clean package -P spark42" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "Installation incomplete. Please:" -ForegroundColor Yellow
    if (-not $javaInstalled) {
        Write-Host "  1. Install Java 11 manually from https://www.oracle.com/java/technologies/downloads/#java11" -ForegroundColor Cyan
    }
    if (-not $mvnInstalled) {
        Write-Host "  2. Install Maven manually from https://maven.apache.org/download.cgi" -ForegroundColor Cyan
    }
    Write-Host "  3. Close and reopen PowerShell" -ForegroundColor Cyan
    Write-Host "  4. Run this script again" -ForegroundColor Cyan
}
