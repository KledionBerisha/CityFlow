package com.cityflow.spark.utils;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for deserializing Avro messages from Kafka with Schema Registry support
 */
public class AvroDeserializer implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AvroDeserializer.class);
    private static final long serialVersionUID = 1L;
    
    private final String schemaRegistryUrl;
    private transient SchemaRegistryClient schemaRegistryClient;
    private transient KafkaAvroDeserializer deserializer;
    
    public AvroDeserializer(String schemaRegistryUrl) {
        this.schemaRegistryUrl = schemaRegistryUrl;
        initializeDeserializer();
    }
    
    private void initializeDeserializer() {
        if (schemaRegistryClient == null) {
            schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 100);
            logger.info("Schema Registry client initialized: {}", schemaRegistryUrl);
        }
        
        if (deserializer == null) {
            Map<String, Object> config = new HashMap<>();
            config.put("schema.registry.url", schemaRegistryUrl);
            config.put("specific.avro.reader", false);
            
            deserializer = new KafkaAvroDeserializer(schemaRegistryClient, config);
            logger.info("Avro deserializer initialized");
        }
    }
    
    /**
     * Read Kafka stream with Avro deserialization
     */
    public Dataset<Row> readKafkaAvroStream(
            SparkSession spark,
            String bootstrapServers,
            String topic,
            String checkpointLocation) {
        
        logger.info("Reading Kafka Avro stream from topic: {}", topic);
        
        // Read binary data from Kafka
        Dataset<Row> kafkaStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", bootstrapServers)
            .option("subscribe", topic)
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 10000)
            .option("failOnDataLoss", false)
            .load();
        
        // Register UDF for Avro deserialization
        spark.udf().register("deserializeAvro", 
            (byte[] bytes) -> {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    if (deserializer == null) {
                        initializeDeserializer();
                    }
                    GenericRecord record = (GenericRecord) deserializer.deserialize(topic, bytes);
                    return record != null ? record.toString() : null;
                } catch (Exception e) {
                    logger.error("Error deserializing Avro message", e);
                    return null;
                }
            }, DataTypes.StringType);
        
        // Deserialize the value column
        return kafkaStream.selectExpr(
            "CAST(key AS STRING) as key",
            "deserializeAvro(value) as value",
            "topic",
            "partition",
            "offset",
            "timestamp",
            "timestampType"
        );
    }
    
    /**
     * Parse JSON string to structured format
     * This is used after Avro deserialization to convert to Spark SQL format
     */
    public Dataset<Row> parseJsonToStruct(Dataset<Row> df, StructType schema) {
        return df.select(
            functions.col("key"),
            functions.from_json(functions.col("value"), schema).as("data"),
            functions.col("topic"),
            functions.col("partition"),
            functions.col("offset"),
            functions.col("timestamp")
        ).select("data.*", "key", "topic", "partition", "offset", "timestamp");
    }
    
    /**
     * Create schema for Traffic Reading Event
     */
    public static StructType getTrafficReadingSchema() {
        return new StructType()
            .add("eventId", DataTypes.StringType, false)
            .add("eventType", DataTypes.StringType, true)
            .add("timestamp", DataTypes.LongType, false)
            .add("sensorId", DataTypes.StringType, false)
            .add("sensorCode", DataTypes.StringType, false)
            .add("roadSegmentId", DataTypes.StringType, false)
            .add("location", new StructType()
                .add("latitude", DataTypes.DoubleType, false)
                .add("longitude", DataTypes.DoubleType, false))
            .add("averageSpeed", DataTypes.DoubleType, true)
            .add("vehicleCount", DataTypes.IntegerType, true)
            .add("occupancy", DataTypes.DoubleType, true)
            .add("congestionLevel", DataTypes.StringType, false)
            .add("queueLength", DataTypes.IntegerType, true)
            .add("temperature", DataTypes.DoubleType, true)
            .add("weatherCondition", DataTypes.StringType, true)
            .add("incidentDetected", DataTypes.BooleanType, false)
            .add("metadata", DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType), true);
    }
    
    /**
     * Create schema for Bus Location Event
     */
    public static StructType getBusLocationSchema() {
        return new StructType()
            .add("eventId", DataTypes.StringType, false)
            .add("eventType", DataTypes.StringType, true)
            .add("timestamp", DataTypes.LongType, false)
            .add("busId", DataTypes.StringType, false)
            .add("vehicleId", DataTypes.StringType, false)
            .add("busCode", DataTypes.StringType, false)
            .add("routeId", DataTypes.StringType, false)
            .add("location", new StructType()
                .add("latitude", DataTypes.DoubleType, false)
                .add("longitude", DataTypes.DoubleType, false)
                .add("altitude", DataTypes.DoubleType, true)
                .add("accuracy", DataTypes.DoubleType, true))
            .add("speedKmh", DataTypes.DoubleType, true)
            .add("heading", DataTypes.DoubleType, true)
            .add("occupancy", DataTypes.IntegerType, true)
            .add("capacity", DataTypes.IntegerType, true)
            .add("operationalStatus", DataTypes.StringType, false)
            .add("nextStopId", DataTypes.StringType, true)
            .add("estimatedArrival", DataTypes.LongType, true)
            .add("delayMinutes", DataTypes.IntegerType, true)
            .add("source", DataTypes.StringType, false)
            .add("metadata", DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType), true);
    }
    
    /**
     * Create schema for Incident Event
     */
    public static StructType getIncidentSchema() {
        return new StructType()
            .add("eventId", DataTypes.StringType, false)
            .add("eventType", DataTypes.StringType, false)
            .add("timestamp", DataTypes.LongType, false)
            .add("incidentId", DataTypes.StringType, false)
            .add("incidentCode", DataTypes.StringType, false)
            .add("type", DataTypes.StringType, false)
            .add("severity", DataTypes.StringType, false)
            .add("status", DataTypes.StringType, false)
            .add("title", DataTypes.StringType, false)
            .add("description", DataTypes.StringType, true)
            .add("location", new StructType()
                .add("latitude", DataTypes.DoubleType, false)
                .add("longitude", DataTypes.DoubleType, false)
                .add("address", DataTypes.StringType, true))
            .add("roadSegmentId", DataTypes.StringType, false)
            .add("sourceId", DataTypes.StringType, false)
            .add("sourceType", DataTypes.StringType, false)
            .add("detectedAt", DataTypes.LongType, false)
            .add("resolvedAt", DataTypes.LongType, true)
            .add("confidence", DataTypes.DoubleType, false)
            .add("impact", new StructType()
                .add("affectedVehicles", DataTypes.IntegerType, true)
                .add("affectedBuses", DataTypes.IntegerType, true)
                .add("estimatedDelayMinutes", DataTypes.IntegerType, true)
                .add("affectedRoutes", DataTypes.createArrayType(DataTypes.StringType), true))
            .add("metadata", DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType), true);
    }
}
