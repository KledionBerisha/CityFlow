package com.cityflow.spark.jobs;

import com.cityflow.spark.config.ConfigLoader;
import com.cityflow.spark.utils.AvroDeserializer;
import com.cityflow.spark.utils.DeltaLakeWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

/**
 * Traffic Aggregation Job
 * Calculates rolling averages for traffic metrics over 5, 15, and 30-minute windows
 * Outputs to PostgreSQL, Redis, and Delta Lake
 */
public class TrafficAggregationJob {
    
    private static final Logger logger = LoggerFactory.getLogger(TrafficAggregationJob.class);
    
    public static void main(String[] args) {
        logger.info("Starting Traffic Aggregation Job...");
        
        // Load configuration
        ConfigLoader config = new ConfigLoader();
        
        // Create Spark session with Delta Lake support
        SparkSession spark = SparkSession.builder()
            .appName("CityFlow-TrafficAggregation")
            .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
            .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
            .config("spark.sql.streaming.schemaInference", "true")
            .getOrCreate();
        
        try {
            // Read traffic events from Kafka
            Dataset<Row> trafficEvents = readTrafficEvents(spark, config);
            
            // Calculate aggregations for different time windows
            Dataset<Row> aggregated5Min = calculateAggregations(trafficEvents, config.getShortWindow(), "5min");
            Dataset<Row> aggregated15Min = calculateAggregations(trafficEvents, config.getMediumWindow(), "15min");
            Dataset<Row> aggregated30Min = calculateAggregations(trafficEvents, config.getLongWindow(), "30min");
            
            // Write to Delta Lake
            DeltaLakeWriter deltaWriter = new DeltaLakeWriter(spark);
            
            StreamingQuery query5Min = deltaWriter.writeStream(
                aggregated5Min,
                config.getTrafficDeltaPath() + "/aggregated_5min",
                config.getCheckpointLocation() + "/traffic_5min",
                "traffic-aggregation-5min",
                config.getTriggerInterval("traffic-aggregation"),
                "date", "hour"
            );
            
            StreamingQuery query15Min = deltaWriter.writeStream(
                aggregated15Min,
                config.getTrafficDeltaPath() + "/aggregated_15min",
                config.getCheckpointLocation() + "/traffic_15min",
                "traffic-aggregation-15min",
                config.getTriggerInterval("traffic-aggregation"),
                "date", "hour"
            );
            
            StreamingQuery query30Min = deltaWriter.writeStream(
                aggregated30Min,
                config.getTrafficDeltaPath() + "/aggregated_30min",
                config.getCheckpointLocation() + "/traffic_30min",
                "traffic-aggregation-30min",
                config.getTriggerInterval("traffic-aggregation"),
                "date", "hour"
            );
            
            // Write to PostgreSQL (30-minute aggregations only)
            writeToPostgreSQL(aggregated30Min, config);
            
            logger.info("All streaming queries started successfully");
            logger.info("Query 5min: {}", query5Min.name());
            logger.info("Query 15min: {}", query15Min.name());
            logger.info("Query 30min: {}", query30Min.name());
            
            // Wait for termination
            spark.streams().awaitAnyTermination();
            
        } catch (Exception e) {
            logger.error("Error in Traffic Aggregation Job", e);
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    /**
     * Read and parse traffic events from Kafka
     */
    private static Dataset<Row> readTrafficEvents(SparkSession spark, ConfigLoader config) {
        logger.info("Reading traffic events from Kafka...");
        
        Dataset<Row> rawStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getKafkaBootstrapServers())
            .option("subscribe", config.getTrafficReadingTopic())
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 10000)
            .option("failOnDataLoss", false)
            .load();
        
        // Parse JSON (Avro would be deserialized to JSON string)
        Dataset<Row> parsed = rawStream
            .selectExpr("CAST(value AS STRING) as json_value")
            .select(from_json(col("json_value"), AvroDeserializer.getTrafficReadingSchema()).as("data"))
            .select("data.*");
        
        // Add watermark for late data
        Dataset<Row> withWatermark = parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withWatermark("event_time", config.getWatermark("traffic"));
        
        // Extract geolocation
        Dataset<Row> withGeo = withWatermark
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
        
        logger.info("Traffic events schema:");
        withGeo.printSchema();
        
        return withGeo;
    }
    
    /**
     * Calculate aggregations for a specific time window
     */
    private static Dataset<Row> calculateAggregations(
            Dataset<Row> events,
            String windowDuration,
            String windowName) {
        
        logger.info("Calculating {} aggregations...", windowName);
        
        Dataset<Row> aggregated = events
            .groupBy(
                window(col("event_time"), windowDuration),
                col("roadSegmentId"),
                col("sensorCode")
            )
            .agg(
                // Speed metrics
                avg("averageSpeed").as("avg_speed"),
                min("averageSpeed").as("min_speed"),
                max("averageSpeed").as("max_speed"),
                stddev("averageSpeed").as("stddev_speed"),
                
                // Vehicle count metrics
                sum("vehicleCount").as("total_vehicles"),
                avg("vehicleCount").as("avg_vehicle_count"),
                max("vehicleCount").as("max_vehicle_count"),
                
                // Occupancy metrics
                avg("occupancy").as("avg_occupancy"),
                max("occupancy").as("max_occupancy"),
                
                // Queue metrics
                avg("queueLength").as("avg_queue_length"),
                max("queueLength").as("max_queue_length"),
                
                // Congestion distribution
                count(when(col("congestionLevel").equalTo("FREE_FLOW"), 1)).as("free_flow_count"),
                count(when(col("congestionLevel").equalTo("LIGHT"), 1)).as("light_congestion_count"),
                count(when(col("congestionLevel").equalTo("MODERATE"), 1)).as("moderate_congestion_count"),
                count(when(col("congestionLevel").equalTo("HEAVY"), 1)).as("heavy_congestion_count"),
                count(when(col("congestionLevel").equalTo("SEVERE"), 1)).as("severe_congestion_count"),
                
                // Incident metrics
                sum(when(col("incidentDetected"), 1).otherwise(0)).as("incident_count"),
                
                // Record count
                count("*").as("reading_count"),
                
                // First and last timestamps
                min("event_time").as("window_start_actual"),
                max("event_time").as("window_end_actual"),
                
                // Average location (centroid)
                avg("latitude").as("avg_latitude"),
                avg("longitude").as("avg_longitude")
            )
            .select(
                col("window.start").as("window_start"),
                col("window.end").as("window_end"),
                lit(windowName).as("window_type"),
                col("roadSegmentId"),
                col("sensorCode"),
                col("avg_speed"),
                col("min_speed"),
                col("max_speed"),
                col("stddev_speed"),
                col("total_vehicles"),
                col("avg_vehicle_count"),
                col("max_vehicle_count"),
                col("avg_occupancy"),
                col("max_occupancy"),
                col("avg_queue_length"),
                col("max_queue_length"),
                col("free_flow_count"),
                col("light_congestion_count"),
                col("moderate_congestion_count"),
                col("heavy_congestion_count"),
                col("severe_congestion_count"),
                col("incident_count"),
                col("reading_count"),
                col("window_start_actual"),
                col("window_end_actual"),
                col("avg_latitude"),
                col("avg_longitude"),
                current_timestamp().as("processed_at")
            )
            // Add partitioning columns
            .withColumn("date", to_date(col("window_start")))
            .withColumn("hour", hour(col("window_start")));
        
        return aggregated;
    }
    
    /**
     * Write aggregated data to PostgreSQL
     */
    private static void writeToPostgreSQL(Dataset<Row> aggregated, ConfigLoader config) {
        logger.info("Writing to PostgreSQL...");
        
        aggregated
            .writeStream()
            .foreachBatch((batchDF, batchId) -> {
                logger.info("Writing batch {} to PostgreSQL", batchId);
                
                batchDF
                    .write()
                    .format("jdbc")
                    .option("url", config.getPostgresUrl())
                    .option("dbtable", "traffic_aggregated_30min")
                    .option("user", config.getPostgresUser())
                    .option("password", config.getPostgresPassword())
                    .option("driver", config.getPostgresDriver())
                    .mode("append")
                    .save();
                
                logger.info("Batch {} written successfully", batchId);
            })
            .trigger(Trigger.ProcessingTime(config.getTriggerInterval("traffic-aggregation")))
            .option("checkpointLocation", config.getCheckpointLocation() + "/traffic_postgres")
            .start();
    }
}
