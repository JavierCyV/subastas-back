# Carga variables de entorno desde .env
Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
    }
}

# Buscar dinámicamente un JDK instalado en Microsoft, Eclipse Adoptium o Java
$jdkPaths = @(
    "C:\Program Files\Microsoft",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Java"
)
$jdk = Get-ChildItem -Path $jdkPaths -Filter "jdk-*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
} else {
    # Ruta por defecto esperada para el OpenJDK 21 instalado
    $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
}

$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Buscar Maven en la carpeta de subastas_back o wrapper
$mvn = "C:\subastas_back\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    # Intento fallback por si acaso
    $mvn = "mvn"
}

Set-Location $PSScriptRoot
Write-Host "Iniciando backend con Java en: $env:JAVA_HOME"
Write-Host "Usando Maven en: $mvn"
& $mvn clean spring-boot:run
