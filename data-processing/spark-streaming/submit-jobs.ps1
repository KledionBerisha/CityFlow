###############################################################################
# CityFlow Spark Streaming Jobs - Submission Script (PowerShell)
# This script submits all Spark streaming jobs to the cluster
###############################################################################

param(
    [Parameter(Position=0)]
    [string]$JobName = "all",
    
    [switch]$Stop,
    [switch]$List,
    [switch]$Help
)

# Configuration
$SparkMaster = if ($env:SPARK_MASTER) { $env:SPARK_MASTER } else { "spark://localhost:7077" }
$JarPath = "target\spark-streaming-jobs-1.0.0.jar"
$DriverMemory = "1g"
$ExecutorMemory = "2g"
$ExecutorCores = "2"
$NumExecutors = "2"
$PidFile = "$env:TEMP\cityflow-spark-jobs.pids"

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

# Function to check if JAR exists
function Test-Jar {
    if (-not (Test-Path $JarPath)) {
        Write-Error-Message "JAR file not found: $JarPath"
        Write-Info "Please build the project first: mvn clean package"
        exit 1
    }
    Write-Success "JAR file found: $JarPath"
}

# Function to submit a Spark job
function Submit-Job {
    param(
        [string]$JobClass,
        [string]$JobName
    )
    
    Write-Info "Submitting ${JobName}..."
    
    $process = Start-Process -FilePath "spark-submit" -ArgumentList @(
        "--master", $SparkMaster,
        "--deploy-mode", "client",
        "--name", $JobName,
        "--driver-memory", $DriverMemory,
        "--executor-memory", $ExecutorMemory,
        "--executor-cores", $ExecutorCores,
        "--num-executors", $NumExecutors,
        "--conf", "spark.streaming.stopGracefullyOnShutdown=true",
        "--conf", "spark.sql.streaming.checkpointLocation=/tmp/cityflow/checkpoints",
        "--conf", "spark.sql.shuffle.partitions=10",
        "--conf", "spark.default.parallelism=10",
        "--packages", "io.delta:delta-spark_2.12:3.0.0,org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0,org.mongodb.spark:mongo-spark-connector_2.12:10.2.0,org.postgresql:postgresql:42.7.1,io.confluent:kafka-avro-serializer:7.5.0",
        "--class", $JobClass,
        $JarPath
    ) -PassThru -NoNewWindow
    
    $process.Id | Add-Content -Path $PidFile
    Write-Success "${JobName} submitted (PID: $($process.Id))"
}

# Function to list running jobs
function Get-RunningJobs {
    Write-Info "Checking running Spark jobs..."
    
    if (Test-Path $PidFile) {
        Get-Content $PidFile | ForEach-Object {
            $pid = [int]$_
            if (Get-Process -Id $pid -ErrorAction SilentlyContinue) {
                Write-Info "Job running with PID: $pid"
            }
        }
    } else {
        Write-Warning-Message "No PID file found"
    }
}

# Function to stop all jobs
function Stop-Jobs {
    Write-Warning-Message "Stopping all CityFlow Spark jobs..."
    
    if (Test-Path $PidFile) {
        Get-Content $PidFile | ForEach-Object {
            $pid = [int]$_
            if (Get-Process -Id $pid -ErrorAction SilentlyContinue) {
                Write-Info "Stopping job with PID: $pid"
                Stop-Process -Id $pid -Force
            }
        }
        
        Remove-Item $PidFile
        Write-Success "All jobs stopped"
    } else {
        Write-Warning-Message "No running jobs found"
    }
}

# Show help
function Show-Help {
    Write-Host "Usage: .\submit-jobs.ps1 [OPTIONS] [JOB_NAME]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Stop       Stop all running jobs"
    Write-Host "  -List       List running jobs"
    Write-Host "  -Help       Show this help message"
    Write-Host ""
    Write-Host "Job Names:"
    Write-Host "  data-lake   Submit Data Lake Ingestion Job"
    Write-Host "  traffic     Submit Traffic Aggregation Job"
    Write-Host "  bus         Submit Bus ETL Job"
    Write-Host "  analytics   Submit Real-Time Analytics Job"
    Write-Host "  all         Submit all jobs (default)"
    Write-Host ""
}

# Main execution
function Main {
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Blue
    Write-Host "║        CityFlow Spark Streaming Jobs Submission           ║" -ForegroundColor Blue
    Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Blue
    
    # Handle flags
    if ($Help) {
        Show-Help
        exit 0
    }
    
    if ($Stop) {
        Stop-Jobs
        exit 0
    }
    
    if ($List) {
        Get-RunningJobs
        exit 0
    }
    
    # Check JAR
    Test-Jar
    
    # Clear old PID file
    if (Test-Path $PidFile) {
        Remove-Item $PidFile
    }
    
    Write-Info "Spark Master: $SparkMaster"
    Write-Info "Starting job submissions...`n"
    
    # Submit jobs based on parameter
    switch ($JobName.ToLower()) {
        "data-lake" {
            Submit-Job -JobClass "com.cityflow.spark.jobs.DataLakeIngestionJob" -JobName "CityFlow-DataLakeIngestion"
        }
        "traffic" {
            Submit-Job -JobClass "com.cityflow.spark.jobs.TrafficAggregationJob" -JobName "CityFlow-TrafficAggregation"
        }
        "bus" {
            Submit-Job -JobClass "com.cityflow.spark.jobs.BusETLJob" -JobName "CityFlow-BusETL"
        }
        "analytics" {
            Submit-Job -JobClass "com.cityflow.spark.jobs.RealTimeAnalyticsJob" -JobName "CityFlow-RealTimeAnalytics"
        }
        default {
            Submit-Job -JobClass "com.cityflow.spark.jobs.DataLakeIngestionJob" -JobName "CityFlow-DataLakeIngestion"
            Start-Sleep -Seconds 5
            Submit-Job -JobClass "com.cityflow.spark.jobs.TrafficAggregationJob" -JobName "CityFlow-TrafficAggregation"
            Start-Sleep -Seconds 5
            Submit-Job -JobClass "com.cityflow.spark.jobs.BusETLJob" -JobName "CityFlow-BusETL"
            Start-Sleep -Seconds 5
            Submit-Job -JobClass "com.cityflow.spark.jobs.RealTimeAnalyticsJob" -JobName "CityFlow-RealTimeAnalytics"
        }
    }
    
    Write-Host ""
    Write-Success "Job submission completed!"
    Write-Info "Spark Master UI: http://localhost:8080"
    Write-Info "Spark History Server: http://localhost:18080"
    Write-Info ""
    Write-Info "To stop all jobs, run: .\submit-jobs.ps1 -Stop"
    Write-Info "To list running jobs, run: .\submit-jobs.ps1 -List"
}

# Run main
Main
