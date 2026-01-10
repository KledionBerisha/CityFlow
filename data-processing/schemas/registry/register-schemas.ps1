###############################################################################
# CityFlow Schema Registry - Schema Registration Script (PowerShell)
# This script registers all Avro schemas with the Confluent Schema Registry
###############################################################################

param(
    [switch]$Delete,
    [switch]$List,
    [string]$SchemaRegistryUrl = "http://localhost:8081"
)

# Configuration
$SchemasDir = Join-Path $PSScriptRoot "..\avro"
$ErrorActionPreference = "Stop"

# Function to print colored messages
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Error-Message {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Warning-Message {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

# Function to check if Schema Registry is available
function Test-SchemaRegistry {
    Write-Info "Checking Schema Registry availability at $SchemaRegistryUrl..."
    
    for ($i = 1; $i -le 30; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$SchemaRegistryUrl/" -Method Get -TimeoutSec 2
            Write-Success "Schema Registry is available!"
            return $true
        }
        catch {
            Write-Warning-Message "Waiting for Schema Registry... (attempt $i/30)"
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Error-Message "Schema Registry is not available after 60 seconds"
    exit 1
}

# Function to register a schema
function Register-Schema {
    param(
        [string]$SchemaFile,
        [string]$Subject
    )
    
    Write-Info "Registering schema: $SchemaFile as subject: $Subject"
    
    try {
        # Read schema file
        $schemaPath = Join-Path $SchemasDir $SchemaFile
        $schemaContent = Get-Content $schemaPath -Raw | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
        
        # Create JSON payload
        $payload = @{
            schema = $schemaContent
            schemaType = "AVRO"
        } | ConvertTo-Json -Depth 100
        
        # Register schema
        $response = Invoke-RestMethod `
            -Uri "$SchemaRegistryUrl/subjects/$Subject/versions" `
            -Method Post `
            -ContentType "application/vnd.schemaregistry.v1+json" `
            -Body $payload
        
        Write-Success "Schema registered successfully with ID: $($response.id)"
        return $true
    }
    catch {
        Write-Error-Message "Failed to register schema: $_"
        return $false
    }
}

# Function to update compatibility level
function Set-Compatibility {
    param(
        [string]$Subject,
        [string]$Level
    )
    
    Write-Info "Setting compatibility level for $Subject to $Level..."
    
    try {
        $payload = @{
            compatibility = $Level
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod `
            -Uri "$SchemaRegistryUrl/config/$Subject" `
            -Method Put `
            -ContentType "application/vnd.schemaregistry.v1+json" `
            -Body $payload
        
        Write-Success "Compatibility level set to: $Level"
    }
    catch {
        Write-Warning-Message "Could not set compatibility level: $_"
    }
}

# Function to list all registered schemas
function Get-RegisteredSchemas {
    Write-Info "Listing all registered schemas..."
    
    try {
        $subjects = Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects" -Method Get
        
        if ($subjects.Count -eq 0) {
            Write-Warning-Message "No schemas registered yet"
            return
        }
        
        Write-Host "`nRegistered Schemas:" -ForegroundColor Blue
        foreach ($subject in $subjects) {
            $latest = Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects/$subject/versions/latest" -Method Get
            Write-Host "  - " -NoNewline
            Write-Host $subject -ForegroundColor Green -NoNewline
            Write-Host " (version: $($latest.version), id: $($latest.id))"
        }
    }
    catch {
        Write-Error-Message "Failed to list schemas: $_"
    }
}

# Function to delete all schemas
function Remove-AllSchemas {
    Write-Warning-Message "Deleting all schemas..."
    
    try {
        $subjects = Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects" -Method Get
        
        foreach ($subject in $subjects) {
            Invoke-RestMethod -Uri "$SchemaRegistryUrl/subjects/$subject" -Method Delete | Out-Null
            Write-Info "Deleted subject: $subject"
        }
        
        Write-Success "All schemas deleted"
    }
    catch {
        Write-Error-Message "Failed to delete schemas: $_"
    }
}

# Main execution
function Main {
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Blue
    Write-Host "║        CityFlow Schema Registry - Registration Tool        ║" -ForegroundColor Blue
    Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Blue
    
    # Check if Schema Registry is available
    Test-SchemaRegistry
    
    # Handle command line arguments
    if ($Delete) {
        Remove-AllSchemas
        exit 0
    }
    elseif ($List) {
        Get-RegisteredSchemas
        exit 0
    }
    
    Write-Info "Starting schema registration process...`n"
    
    # Register schemas with appropriate subject names
    Register-Schema -SchemaFile "traffic-reading-event.avsc" -Subject "traffic.reading.events-value"
    Set-Compatibility -Subject "traffic.reading.events-value" -Level "BACKWARD"
    Write-Host ""
    
    Register-Schema -SchemaFile "bus-location-event.avsc" -Subject "bus.location.events-value"
    Set-Compatibility -Subject "bus.location.events-value" -Level "BACKWARD"
    Write-Host ""
    
    Register-Schema -SchemaFile "incident-event.avsc" -Subject "incident.events-value"
    Set-Compatibility -Subject "incident.events-value" -Level "BACKWARD"
    Write-Host ""
    
    Register-Schema -SchemaFile "sensor-status-event.avsc" -Subject "sensor.status.events-value"
    Set-Compatibility -Subject "sensor.status.events-value" -Level "BACKWARD"
    Write-Host ""
    
    Register-Schema -SchemaFile "bus-status-event.avsc" -Subject "bus.status.events-value"
    Set-Compatibility -Subject "bus.status.events-value" -Level "BACKWARD"
    Write-Host ""
    
    # List all registered schemas
    Get-RegisteredSchemas
    
    Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║         Schema registration completed successfully!        ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Green
    
    Write-Info "Schema Registry UI: http://localhost:8082"
    Write-Info "Kafka Topics UI: http://localhost:8083"
    Write-Info "REST Proxy: http://localhost:8084"
}

# Run main function
Main
