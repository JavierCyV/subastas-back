# Carga variables de entorno desde .env
Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
    }
}

# Buscar dinámicamente un JDK instalado en Eclipse Adoptium o Java
$jdk = Get-ChildItem -Path "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -Filter "jdk-*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
} else {
    # Si no se encuentra, usamos la ruta por defecto esperada para el instalador de Adoptium JDK 25
    $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
}

$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Ruta corregida de Maven para tu usuario actual (Usuario)
$mvn = "C:\Users\Usuario\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"

Set-Location $PSScriptRoot
& $mvn clean spring-boot:run

