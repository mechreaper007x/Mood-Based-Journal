# Load environment variables from .env file
Get-Content -Path "..\.env" | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, [System.EnvironmentVariableTarget]::Process)
        Write-Host "Loaded Env: $name"
    }
}

# Run the Spring Boot application
.\mvnw.cmd spring-boot:run