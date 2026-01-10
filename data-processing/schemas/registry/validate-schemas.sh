#!/bin/bash

###############################################################################
# CityFlow Schema Registry - Schema Validation Script
# This script validates Avro schemas for correctness and compatibility
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCHEMAS_DIR="../avro"
ERRORS=0

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((ERRORS++))
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to validate schema syntax
validate_schema() {
    local schema_file=$1
    local schema_path="${SCHEMAS_DIR}/${schema_file}"
    
    print_info "Validating: ${schema_file}"
    
    # Check if file exists
    if [ ! -f "$schema_path" ]; then
        print_error "Schema file not found: ${schema_path}"
        return 1
    fi
    
    # Validate JSON syntax
    if ! jq empty "$schema_path" 2>/dev/null; then
        print_error "Invalid JSON syntax in ${schema_file}"
        return 1
    fi
    
    # Check required Avro fields
    if ! jq -e '.type' "$schema_path" > /dev/null; then
        print_error "Missing 'type' field in ${schema_file}"
        return 1
    fi
    
    if ! jq -e '.name' "$schema_path" > /dev/null; then
        print_error "Missing 'name' field in ${schema_file}"
        return 1
    fi
    
    if ! jq -e '.namespace' "$schema_path" > /dev/null; then
        print_warning "Missing 'namespace' field in ${schema_file}"
    fi
    
    # Validate field definitions
    if jq -e '.fields' "$schema_path" > /dev/null; then
        field_count=$(jq '.fields | length' "$schema_path")
        if [ "$field_count" -eq 0 ]; then
            print_error "No fields defined in ${schema_file}"
            return 1
        fi
        
        # Check each field has name and type
        for i in $(seq 0 $((field_count - 1))); do
            if ! jq -e ".fields[$i].name" "$schema_path" > /dev/null; then
                print_error "Field at index $i missing 'name' in ${schema_file}"
                return 1
            fi
            if ! jq -e ".fields[$i].type" "$schema_path" > /dev/null; then
                print_error "Field at index $i missing 'type' in ${schema_file}"
                return 1
            fi
        done
    fi
    
    print_success "Schema is valid: ${schema_file}"
    return 0
}

# Function to check schema compatibility
check_compatibility() {
    local old_schema=$1
    local new_schema=$2
    
    print_info "Checking backward compatibility: ${old_schema} -> ${new_schema}"
    
    # This is a basic check - in production, use Schema Registry's compatibility check
    old_fields=$(jq -r '.fields[].name' "${SCHEMAS_DIR}/${old_schema}" | sort)
    new_fields=$(jq -r '.fields[].name' "${SCHEMAS_DIR}/${new_schema}" | sort)
    
    # Check if old fields are present in new schema (backward compatibility)
    while IFS= read -r field; do
        if ! echo "$new_fields" | grep -q "^${field}$"; then
            print_warning "Field '${field}' removed in new schema - may break backward compatibility"
        fi
    done <<< "$old_fields"
    
    print_success "Compatibility check completed"
}

# Function to display schema statistics
show_statistics() {
    local schema_file=$1
    local schema_path="${SCHEMAS_DIR}/${schema_file}"
    
    echo -e "\n${BLUE}Statistics for ${schema_file}:${NC}"
    echo "  Name: $(jq -r '.name' "$schema_path")"
    echo "  Namespace: $(jq -r '.namespace' "$schema_path")"
    echo "  Type: $(jq -r '.type' "$schema_path")"
    
    if jq -e '.doc' "$schema_path" > /dev/null; then
        echo "  Documentation: $(jq -r '.doc' "$schema_path")"
    fi
    
    if jq -e '.fields' "$schema_path" > /dev/null; then
        field_count=$(jq '.fields | length' "$schema_path")
        echo "  Fields: ${field_count}"
        
        # List fields
        echo -e "\n  ${BLUE}Field Details:${NC}"
        for i in $(seq 0 $((field_count - 1))); do
            name=$(jq -r ".fields[$i].name" "$schema_path")
            type=$(jq -r ".fields[$i].type" "$schema_path")
            echo "    - ${name}: ${type}"
        done
    fi
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║         CityFlow Schema Validation Tool                    ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}\n"
    
    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        print_error "jq is not installed. Please install jq to validate schemas."
        exit 1
    fi
    
    print_info "Starting schema validation...\n"
    
    # Validate all schemas
    validate_schema "traffic-reading-event.avsc"
    show_statistics "traffic-reading-event.avsc"
    
    validate_schema "bus-location-event.avsc"
    show_statistics "bus-location-event.avsc"
    
    validate_schema "incident-event.avsc"
    show_statistics "incident-event.avsc"
    
    validate_schema "sensor-status-event.avsc"
    show_statistics "sensor-status-event.avsc"
    
    validate_schema "bus-status-event.avsc"
    show_statistics "bus-status-event.avsc"
    
    # Summary
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    if [ $ERRORS -eq 0 ]; then
        echo -e "${GREEN}║         All schemas validated successfully!                ║${NC}"
    else
        echo -e "${RED}║         Validation completed with ${ERRORS} error(s)               ║${NC}"
    fi
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}\n"
    
    exit $ERRORS
}

# Run main function
main
