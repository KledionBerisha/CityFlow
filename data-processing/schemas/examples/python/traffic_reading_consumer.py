#!/usr/bin/env python3
"""
CityFlow - Traffic Reading Consumer Example (Python)
Demonstrates how to consume TrafficReadingEvent messages from Kafka using Avro deserialization
"""

from confluent_kafka.avro import AvroConsumer
from confluent_kafka import KafkaError
from datetime import datetime
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
GROUP_ID = 'traffic-consumer-python-example'


def create_consumer_config():
    """Create Kafka consumer configuration"""
    return {
        'bootstrap.servers': BOOTSTRAP_SERVERS,
        'schema.registry.url': SCHEMA_REGISTRY_URL,
        'group.id': GROUP_ID,
        'client.id': f'traffic-consumer-python-{int(datetime.now().timestamp())}',
        # Consumer behavior
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': True,
        'auto.commit.interval.ms': 1000,
    }


def process_event(msg):
    """Process received traffic reading event"""
    event = msg.value()
    
    logger.info("=" * 60)
    logger.info("Received Traffic Reading Event:")
    logger.info(f"  Partition: {msg.partition()}, Offset: {msg.offset()}")
    logger.info(f"  Event ID: {event['eventId']}")
    logger.info(f"  Sensor: {event['sensorCode']} ({event['sensorId']})")
    logger.info(f"  Road Segment: {event['roadSegmentId']}")
    logger.info(f"  Location: ({event['location']['latitude']:.6f}, {event['location']['longitude']:.6f})")
    logger.info(f"  Average Speed: {event['averageSpeed']} km/h")
    logger.info(f"  Vehicle Count: {event['vehicleCount']}")
    logger.info(f"  Occupancy: {event['occupancy'] * 100:.2f}%")
    logger.info(f"  Congestion Level: {event['congestionLevel']}")
    logger.info(f"  Queue Length: {event['queueLength']} vehicles")
    logger.info(f"  Temperature: {event['temperature']}°C")
    logger.info(f"  Weather: {event['weatherCondition']}")
    logger.info(f"  Incident Detected: {event['incidentDetected']}")
    
    timestamp = datetime.fromtimestamp(event['timestamp'] / 1000)
    logger.info(f"  Timestamp: {timestamp}")
    
    if event.get('metadata'):
        logger.info(f"  Metadata: {event['metadata']}")
    
    # Business logic
    detect_congestion(event)
    check_for_incidents(event)


def detect_congestion(event):
    """Detect and alert on congestion"""
    congestion_level = event['congestionLevel']
    if congestion_level in ['SEVERE', 'HEAVY']:
        logger.warning(
            f"⚠️  ALERT: Heavy congestion detected on {event['roadSegmentId']} - "
            f"Speed: {event['averageSpeed']} km/h"
        )


def check_for_incidents(event):
    """Check and alert on incidents"""
    if event['incidentDetected']:
        logger.error(
            f"🚨 INCIDENT ALERT: Possible incident at {event['roadSegmentId']} "
            f"({event['location']['latitude']}, {event['location']['longitude']})"
        )


def main():
    """Main consumer logic"""
    logger.info("Starting Traffic Reading Consumer (Python)...")
    
    try:
        # Create consumer
        config = create_consumer_config()
        consumer = AvroConsumer(config)
        
        # Subscribe to topic
        consumer.subscribe([TOPIC])
        logger.info(f"Subscribed to topic: {TOPIC}")
        logger.info("Waiting for messages...")
        
        # Consume messages
        while True:
            msg = consumer.poll(timeout=1.0)
            
            if msg is None:
                continue
            
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    logger.info(f"Reached end of partition {msg.partition()}")
                else:
                    logger.error(f"Consumer error: {msg.error()}")
                continue
            
            # Process the message
            process_event(msg)
    
    except KeyboardInterrupt:
        logger.info("Consumer interrupted by user")
    except Exception as e:
        logger.error(f"Error in consumer: {e}", exc_info=True)
    finally:
        # Clean up
        logger.info("Closing consumer...")
        consumer.close()
        logger.info("Consumer shut down")


if __name__ == "__main__":
    main()
