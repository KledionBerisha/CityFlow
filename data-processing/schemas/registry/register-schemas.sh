#!/bin/bash

###############################################################################
# CityFlow Schema Registry - Schema Registration Script
# This script registers all Avro schemas with the Confluent Schema Registry
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
SCHEMAS_DIR="../avro"

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

# Function to check if Schema Registry is available
check_schema_registry() {
    print_info "Checking Schema Registry availability at ${SCHEMA_REGISTRY_URL}..."
    
    for i in {1..30}; do
        if curl -s -f "${SCHEMA_REGISTRY_URL}/" > /dev/null; then
            print_success "Schema Registry is available!"
            return 0
        fi
        print_warning "Waiting for Schema Registry... (attempt $i/30)"
        sleep 2
    done
    
    print_error "Schema Registry is not available after 60 seconds"
    exit 1
}

# Function to register a schema
register_schema() {
    local schema_file=$1
    local subject=$2
    
    print_info "Registering schema: ${schema_file} as subject: ${subject}"
    
    # Read schema file and escape quotes
    schema_content=$(cat "${SCHEMAS_DIR}/${schema_file}" | jq -c '.')
    
    # Create JSON payload
    payload=$(jq -n \
        --arg schema "$schema_content" \
        '{schema: $schema, schemaType: "AVRO"}')
    
    # Register schema
    response=$(curl -s -X POST \
        -H "Content-Type: application/vnd.schemaregistry.v1+json" \
        --data "$payload" \
        "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions")
    
    # Check response
    if echo "$response" | jq -e '.id' > /dev/null 2>&1; then
        schema_id=$(echo "$response" | jq -r '.id')
        print_success "Schema registered successfully with ID: ${schema_id}"
        return 0
    else
        print_error "Failed to register schema: ${response}"
        return 1
    fi
}

# Function to update compatibility level
set_compatibility() {
    local subject=$1
    local level=$2
    
    print_info "Setting compatibility level for ${subject} to ${level}..."
    
    response=$(curl -s -X PUT \
        -H "Content-Type: application/vnd.schemaregistry.v1+json" \
        --data "{\"compatibility\": \"${level}\"}" \
        "${SCHEMA_REGISTRY_URL}/config/${subject}")
    
    if echo "$response" | jq -e '.compatibility' > /dev/null 2>&1; then
        print_success "Compatibility level set to: ${level}"
    else
        print_warning "Could not set compatibility level: ${response}"
    fi
}

# Function to list all registered schemas
list_schemas() {
    print_info "Listing all registered schemas..."
    
    subjects=$(curl -s "${SCHEMA_REGISTRY_URL}/subjects")
    
    if [ "$subjects" == "[]" ]; then
        print_warning "No schemas registered yet"
        return
    fi
    
    echo -e "\n${BLUE}Registered Schemas:${NC}"
    echo "$subjects" | jq -r '.[]' | while read -r subject; do
        version=$(curl -s "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions/latest" | jq -r '.version')
        id=$(curl -s "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions/latest" | jq -r '.id')
        echo "  - ${GREEN}${subject}${NC} (version: ${version}, id: ${id})"
    done
}

# Function to delete all schemas (for development)
delete_all_schemas() {
    print_warning "Deleting all schemas..."
    
    subjects=$(curl -s "${SCHEMA_REGISTRY_URL}/subjects" | jq -r '.[]')
    
    for subject in $subjects; do
        curl -s -X DELETE "${SCHEMA_REGISTRY_URL}/subjects/${subject}" > /dev/null
        print_info "Deleted subject: ${subject}"
    done
    
    print_success "All schemas deleted"
}

# Main execution
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║        CityFlow Schema Registry - Registration Tool        ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}\n"
    
    # Check if Schema Registry is available
    check_schema_registry
    
    # Parse command line arguments
    if [ "$1" == "--delete" ]; then
        delete_all_schemas
        exit 0
    elif [ "$1" == "--list" ]; then
        list_schemas
        exit 0
    fi
    
    print_info "Starting schema registration process...\n"
    
    # Register schemas with appropriate subject names
    # Subject naming: <topic-name>-value (for value schemas)
    
    register_schema "traffic-reading-event.avsc" "traffic.reading.events-value"
    set_compatibility "traffic.reading.events-value" "BACKWARD"
    echo ""
    
    register_schema "bus-location-event.avsc" "bus.location.events-value"
    set_compatibility "bus.location.events-value" "BACKWARD"
    echo ""
    
    register_schema "incident-event.avsc" "incident.events-value"
    set_compatibility "incident.events-value" "BACKWARD"
    echo ""
    
    register_schema "sensor-status-event.avsc" "sensor.status.events-value"
    set_compatibility "sensor.status.events-value" "BACKWARD"
    echo ""
    
    register_schema "bus-status-event.avsc" "bus.status.events-value"
    set_compatibility "bus.status.events-value" "BACKWARD"
    echo ""
    
    # List all registered schemas
    list_schemas
    
    echo -e "\n${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║         Schema registration completed successfully!        ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}\n"
    
    print_info "Schema Registry UI: http://localhost:8082"
    print_info "Kafka Topics UI: http://localhost:8083"
    print_info "REST Proxy: http://localhost:8084"
}

# Run main function
main "$@"
