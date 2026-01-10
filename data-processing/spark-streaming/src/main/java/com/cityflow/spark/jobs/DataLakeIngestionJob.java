package com.cityflow.spark.jobs;

import com.cityflow.spark.config.ConfigLoader;
import com.cityflow.spark.utils.AvroDeserializer;
import com.cityflow.spark.utils.DeltaLakeWriter;
import com.cityflow.spark.utils.DeadLetterQueueHandler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

/**
 * Data Lake Ingestion Job
 * Consumes all event types from Kafka and persists to Delta Lake
 * Provides a single source of truth for all CityFlow events
 */
public class DataLakeIngestionJob {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLakeIngestionJob.class);
    
    public static void main(String[] args) {
        logger.info("Starting Data Lake Ingestion Job...");
        
        // Load configuration
        ConfigLoader config = new ConfigLoader();
        
        // Create Spark session with Delta Lake support
        SparkSession spark = SparkSession.builder()
            .appName("CityFlow-DataLakeIngestion")
            .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
            .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
            .config("spark.sql.streaming.schemaInference", "true")
            .getOrCreate();
        
        try {
            DeltaLakeWriter deltaWriter = new DeltaLakeWriter(spark);
            DeadLetterQueueHandler dlqHandler = new DeadLetterQueueHandler(
                config.getKafkaBootstrapServers(),
                config.getDeadLetterQueueTopic()
            );
            
            // Ingest all event types
            StreamingQuery trafficQuery = ingestTrafficEvents(spark, config, deltaWriter);
            StreamingQuery busQuery = ingestBusEvents(spark, config, deltaWriter);
            StreamingQuery incidentQuery = ingestIncidentEvents(spark, config, deltaWriter);
            
            logger.info("All ingestion queries started successfully");
            logger.info("Traffic Query: {}", trafficQuery.name());
            logger.info("Bus Query: {}", busQuery.name());
            logger.info("Incident Query: {}", incidentQuery.name());
            
            // Wait for termination
            spark.streams().awaitAnyTermination();
            
        } catch (Exception e) {
            logger.error("Error in Data Lake Ingestion Job", e);
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    /**
     * Ingest traffic reading events
     */
    private static StreamingQuery ingestTrafficEvents(
            SparkSession spark,
            ConfigLoader config,
            DeltaLakeWriter deltaWriter) throws java.util.concurrent.TimeoutException {
        
        logger.info("Starting traffic events ingestion...");
        
        Dataset<Row> rawStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getKafkaBootstrapServers())
            .option("subscribe", config.getTrafficReadingTopic())
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 10000)
            .option("failOnDataLoss", false)
            .load();
        
        // Parse JSON
        Dataset<Row> parsed = rawStream
            .selectExpr("CAST(value AS STRING) as json_value", "timestamp as kafka_timestamp", "partition", "offset")
            .select(
                from_json(col("json_value"), AvroDeserializer.getTrafficReadingSchema()).as("data"),
                col("kafka_timestamp"),
                col("partition"),
                col("offset")
            )
            .select("data.*", "kafka_timestamp", "partition", "offset");
        
        // Add processing metadata
        Dataset<Row> enriched = parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withColumn("ingestion_time", current_timestamp())
            .withColumn("date", to_date(col("event_time")))
            .withColumn("hour", hour(col("event_time")))
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
        
        // Data quality validation
        Dataset<Row> validated = enriched
            .filter(col("eventId").isNotNull())
            .filter(col("sensorId").isNotNull())
            .filter(col("latitude").between(-90, 90))
            .filter(col("longitude").between(-180, 180));
        
        return deltaWriter.writeStream(
            validated,
            config.getTrafficDeltaPath() + "/raw",
            config.getCheckpointLocation() + "/traffic_raw",
            "traffic-ingestion",
            config.getTriggerInterval("data-lake"),
            "date", "hour"
        );
    }
    
    /**
     * Ingest bus location events
     */
    private static StreamingQuery ingestBusEvents(
            SparkSession spark,
            ConfigLoader config,
            DeltaLakeWriter deltaWriter) throws java.util.concurrent.TimeoutException {
        
        logger.info("Starting bus events ingestion...");
        
        Dataset<Row> rawStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getKafkaBootstrapServers())
            .option("subscribe", config.getBusLocationTopic())
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 10000)
            .option("failOnDataLoss", false)
            .load();
        
        // Parse JSON
        Dataset<Row> parsed = rawStream
            .selectExpr("CAST(value AS STRING) as json_value", "timestamp as kafka_timestamp", "partition", "offset")
            .select(
                from_json(col("json_value"), AvroDeserializer.getBusLocationSchema()).as("data"),
                col("kafka_timestamp"),
                col("partition"),
                col("offset")
            )
            .select("data.*", "kafka_timestamp", "partition", "offset");
        
        // Add processing metadata
        Dataset<Row> enriched = parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withColumn("ingestion_time", current_timestamp())
            .withColumn("date", to_date(col("event_time")))
            .withColumn("hour", hour(col("event_time")))
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
        
        // Data quality validation
        Dataset<Row> validated = enriched
            .filter(col("eventId").isNotNull())
            .filter(col("busId").isNotNull())
            .filter(col("latitude").between(-90, 90))
            .filter(col("longitude").between(-180, 180));
        
        return deltaWriter.writeStream(
            validated,
            config.getBusDeltaPath() + "/raw",
            config.getCheckpointLocation() + "/bus_raw",
            "bus-ingestion",
            config.getTriggerInterval("data-lake"),
            "date", "hour", "routeId"
        );
    }
    
    /**
     * Ingest incident events
     */
    private static StreamingQuery ingestIncidentEvents(
            SparkSession spark,
            ConfigLoader config,
            DeltaLakeWriter deltaWriter) throws java.util.concurrent.TimeoutException {
        
        logger.info("Starting incident events ingestion...");
        
        Dataset<Row> rawStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getKafkaBootstrapServers())
            .option("subscribe", config.getIncidentTopic())
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 10000)
            .option("failOnDataLoss", false)
            .load();
        
        // Parse JSON
        Dataset<Row> parsed = rawStream
            .selectExpr("CAST(value AS STRING) as json_value", "timestamp as kafka_timestamp", "partition", "offset")
            .select(
                from_json(col("json_value"), AvroDeserializer.getIncidentSchema()).as("data"),
                col("kafka_timestamp"),
                col("partition"),
                col("offset")
            )
            .select("data.*", "kafka_timestamp", "partition", "offset");
        
        // Add processing metadata
        Dataset<Row> enriched = parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withColumn("ingestion_time", current_timestamp())
            .withColumn("date", to_date(col("event_time")))
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
        
        // Data quality validation
        Dataset<Row> validated = enriched
            .filter(col("eventId").isNotNull())
            .filter(col("incidentId").isNotNull())
            .filter(col("latitude").between(-90, 90))
            .filter(col("longitude").between(-180, 180));
        
        return deltaWriter.writeStream(
            validated,
            config.getIncidentDeltaPath() + "/raw",
            config.getCheckpointLocation() + "/incident_raw",
            "incident-ingestion",
            config.getTriggerInterval("data-lake"),
            "date", "severity"
        );
    }
}
