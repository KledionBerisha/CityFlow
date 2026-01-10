#!/usr/bin/env python3
"""
CityFlow - Traffic Reading Producer Example (Python)
Demonstrates how to produce TrafficReadingEvent messages to Kafka using Avro serialization
"""

import time
import uuid
import random
from datetime import datetime
from confluent_kafka import Producer
from confluent_kafka.avro import AvroProducer
from confluent_kafka.avro import load as avro_load
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configuration
BOOTSTRAP_SERVERS = 'localhost:9094'
SCHEMA_REGISTRY_URL = 'http://localhost:8081'
TOPIC = 'traffic.reading.events'
SCHEMA_FILE = '../../avro/traffic-reading-event.avsc'


def create_producer_config():
    """Create Kafka producer configuration"""
    return {
        'bootstrap.servers': BOOTSTRAP_SERVERS,
        'schema.registry.url': SCHEMA_REGISTRY_URL,
        'client.id': 'traffic-producer-python-example',
        # Performance tuning
        'compression.type': 'snappy',
        'acks': 1,
        'retries': 3,
    }


def load_avro_schema():
    """Load Avro schema from file"""
    try:
        with open(SCHEMA_FILE, 'r') as f:
            return avro_load(f)
    except FileNotFoundError:
        logger.error(f"Schema file not found: {SCHEMA_FILE}")
        raise


def determine_congestion_level(speed):
    """Determine congestion level based on speed"""
    if speed >= 80:
        return "FREE_FLOW"
    elif speed >= 60:
        return "LIGHT"
    elif speed >= 40:
        return "MODERATE"
    elif speed >= 20:
        return "HEAVY"
    else:
        return "SEVERE"


def create_sample_event(index):
    """Create a sample traffic reading event"""
    speed = random.uniform(20, 100)  # 20-100 km/h
    congestion_level = determine_congestion_level(speed)
    
    return {
        "eventId": str(uuid.uuid4()),
        "eventType": "TRAFFIC_READING",
        "timestamp": int(datetime.now().timestamp() * 1000),
        "sensorId": f"sensor-id-{index + 1}",
        "sensorCode": f"SENSOR-{index + 1:03d}",
        "roadSegmentId": f"ROAD-SEG-{(index % 5) + 1:03d}",
        "location": {
            "latitude": 42.6521 + (random.random() * 0.01),
            "longitude": 21.1657 + (random.random() * 0.01)
        },
        "averageSpeed": round(speed, 2),
        "vehicleCount": random.randint(10, 100),
        "occupancy": round(random.random(), 2),
        "congestionLevel": congestion_level,
        "queueLength": random.randint(20, 50) if congestion_level == "SEVERE" else random.randint(0, 10),
        "temperature": round(15 + random.random() * 15, 1),  # 15-30°C
        "weatherCondition": "CLEAR",
        "incidentDetected": random.random() < 0.1,  # 10% chance
        "metadata": {
            "source": "simulator",
            "version": "1.0"
        }
    }


def delivery_callback(err, msg):
    """Callback for delivery reports"""
    if err:
        logger.error(f'Message delivery failed: {err}')
    else:
        logger.info(f'Message delivered to {msg.topic()} [partition {msg.partition()}] @ offset {msg.offset()}')


def main():
    """Main producer logic"""
    logger.info("Starting Traffic Reading Producer (Python)...")
    
    try:
        # Load schema
        schema = load_avro_schema()
        logger.info("Avro schema loaded successfully")
        
        # Create producer
        config = create_producer_config()
        producer = AvroProducer(
            config,
            default_value_schema=schema
        )
        logger.info("Producer created successfully")
        
        # Produce 10 sample events
        for i in range(10):
            event = create_sample_event(i)
            
            # Use sensor code as key for partitioning
            key = event['sensorCode']
            
            logger.info(f"\n{'='*60}")
            logger.info(f"Producing event {i+1}/10:")
            logger.info(f"  Sensor: {event['sensorCode']}")
            logger.info(f"  Speed: {event['averageSpeed']} km/h")
            logger.info(f"  Congestion: {event['congestionLevel']}")
            logger.info(f"  Vehicles: {event['vehicleCount']}")
            
            # Produce message
            producer.produce(
                topic=TOPIC,
                key=key,
                value=event,
                callback=delivery_callback
            )
            
            # Trigger delivery callbacks
            producer.poll(0)
            
            # Wait before sending next message
            time.sleep(1)
        
        # Wait for all messages to be delivered
        logger.info("\nFlushing producer...")
        producer.flush()
        logger.info("All messages sent successfully!")
        
    except KeyboardInterrupt:
        logger.info("Producer interrupted by user")
    except Exception as e:
        logger.error(f"Error in producer: {e}", exc_info=True)
    finally:
        logger.info("Producer shutting down")


if __name__ == "__main__":
    main()
