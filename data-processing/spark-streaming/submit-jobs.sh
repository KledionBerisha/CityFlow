#!/bin/bash

###############################################################################
# CityFlow Spark Streaming Jobs - Submission Script
# This script submits all Spark streaming jobs to the cluster
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SPARK_MASTER="${SPARK_MASTER:-spark://localhost:7077}"
JAR_PATH="target/spark-streaming-jobs-1.0.0.jar"
DRIVER_MEMORY="1g"
EXECUTOR_MEMORY="2g"
EXECUTOR_CORES="2"
NUM_EXECUTORS="2"

# Function to print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to check if JAR exists
check_jar() {
    if [ ! -f "$JAR_PATH" ]; then
        print_error "JAR file not found: $JAR_PATH"
        print_info "Please build the project first: mvn clean package"
        exit 1
    fi
    print_success "JAR file found: $JAR_PATH"
}

# Function to submit a Spark job
submit_job() {
    local job_class=$1
    local job_name=$2
    
    print_info "Submitting ${job_name}..."
    
    spark-submit \
        --master "$SPARK_MASTER" \
        --deploy-mode client \
        --name "$job_name" \
        --driver-memory "$DRIVER_MEMORY" \
        --executor-memory "$EXECUTOR_MEMORY" \
        --executor-cores "$EXECUTOR_CORES" \
        --num-executors "$NUM_EXECUTORS" \
        --conf "spark.streaming.stopGracefullyOnShutdown=true" \
        --conf "spark.sql.streaming.checkpointLocation=/tmp/cityflow/checkpoints" \
        --conf "spark.sql.shuffle.partitions=10" \
        --conf "spark.default.parallelism=10" \
        --packages "io.delta:delta-spark_2.12:3.0.0,org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0,org.mongodb.spark:mongo-spark-connector_2.12:10.2.0,org.postgresql:postgresql:42.7.1,io.confluent:kafka-avro-serializer:7.5.0" \
        --class "$job_class" \
        "$JAR_PATH" \
        > /dev/null 2>&1 &
    
    local pid=$!
    print_success "${job_name} submitted (PID: ${pid})"
    echo $pid >> /tmp/cityflow-spark-jobs.pids
}

# Function to list running jobs
list_jobs() {
    print_info "Checking running Spark jobs..."
    
    if [ -f /tmp/cityflow-spark-jobs.pids ]; then
        while read pid; do
            if ps -p $pid > /dev/null 2>&1; then
                print_info "Job running with PID: $pid"
            fi
        done < /tmp/cityflow-spark-jobs.pids
    else
        print_warning "No PID file found"
    fi
}

# Function to stop all jobs
stop_jobs() {
    print_warning "Stopping all CityFlow Spark jobs..."
    
    if [ -f /tmp/cityflow-spark-jobs.pids ]; then
        while read pid; do
            if ps -p $pid > /dev/null 2>&1; then
                print_info "Stopping job with PID: $pid"
                kill $pid
            fi
        done < /tmp/cityflow-spark-jobs.pids
        
        rm /tmp/cityflow-spark-jobs.pids
        print_success "All jobs stopped"
    else
        print_warning "No running jobs found"
    fi
}

# Main execution
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║        CityFlow Spark Streaming Jobs Submission           ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}\n"
    
    # Parse command line arguments
    case "${1:-}" in
        --stop)
            stop_jobs
            exit 0
            ;;
        --list)
            list_jobs
            exit 0
            ;;
        --help)
            echo "Usage: $0 [OPTIONS] [JOB_NAME]"
            echo ""
            echo "Options:"
            echo "  --stop    Stop all running jobs"
            echo "  --list    List running jobs"
            echo "  --help    Show this help message"
            echo ""
            echo "Job Names:"
            echo "  data-lake    Submit Data Lake Ingestion Job"
            echo "  traffic      Submit Traffic Aggregation Job"
            echo "  bus          Submit Bus ETL Job"
            echo "  analytics    Submit Real-Time Analytics Job"
            echo "  all          Submit all jobs (default)"
            echo ""
            exit 0
            ;;
    esac
    
    # Check JAR
    check_jar
    
    # Clear old PID file
    rm -f /tmp/cityflow-spark-jobs.pids
    
    print_info "Spark Master: $SPARK_MASTER"
    print_info "Starting job submissions...\n"
    
    # Determine which jobs to submit
    job_to_submit="${1:-all}"
    
    case "$job_to_submit" in
        data-lake)
            submit_job "com.cityflow.spark.jobs.DataLakeIngestionJob" "CityFlow-DataLakeIngestion"
            ;;
        traffic)
            submit_job "com.cityflow.spark.jobs.TrafficAggregationJob" "CityFlow-TrafficAggregation"
            ;;
        bus)
            submit_job "com.cityflow.spark.jobs.BusETLJob" "CityFlow-BusETL"
            ;;
        analytics)
            submit_job "com.cityflow.spark.jobs.RealTimeAnalyticsJob" "CityFlow-RealTimeAnalytics"
            ;;
        all|*)
            submit_job "com.cityflow.spark.jobs.DataLakeIngestionJob" "CityFlow-DataLakeIngestion"
            sleep 5
            submit_job "com.cityflow.spark.jobs.TrafficAggregationJob" "CityFlow-TrafficAggregation"
            sleep 5
            submit_job "com.cityflow.spark.jobs.BusETLJob" "CityFlow-BusETL"
            sleep 5
            submit_job "com.cityflow.spark.jobs.RealTimeAnalyticsJob" "CityFlow-RealTimeAnalytics"
            ;;
    esac
    
    echo ""
    print_success "Job submission completed!"
    print_info "Spark Master UI: http://localhost:8080"
    print_info "Spark History Server: http://localhost:18080"
    print_info ""
    print_info "To stop all jobs, run: $0 --stop"
    print_info "To list running jobs, run: $0 --list"
}

# Run main function
main "$@"
