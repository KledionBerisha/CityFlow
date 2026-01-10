package com.cityflow.spark.jobs;

import com.cityflow.spark.config.ConfigLoader;
import com.cityflow.spark.utils.AvroDeserializer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.apache.spark.sql.functions.*;

/**
 * Real-time Analytics Job
 * Generates live KPIs and dashboard metrics
 * Updates Redis cache with latest analytics
 */
public class RealTimeAnalyticsJob {
    
    private static final Logger logger = LoggerFactory.getLogger(RealTimeAnalyticsJob.class);
    
    public static void main(String[] args) {
        logger.info("Starting Real-Time Analytics Job...");
        
        // Load configuration
        ConfigLoader config = new ConfigLoader();
        
        // Create Spark session
        SparkSession spark = SparkSession.builder()
            .appName("CityFlow-RealTimeAnalytics")
            .config("spark.sql.streaming.schemaInference", "true")
            .getOrCreate();
        
        try {
            // Read traffic events
            Dataset<Row> trafficEvents = readTrafficEvents(spark, config);
            
            // Calculate real-time KPIs
            Dataset<Row> kpis = calculateKPIs(trafficEvents);
            
            // Write to Redis
            StreamingQuery query = writeToRedis(kpis, config);
            
            logger.info("Real-time Analytics query started: {}", query.name());
            
            // Wait for termination
            spark.streams().awaitAnyTermination();
            
        } catch (Exception e) {
            logger.error("Error in Real-Time Analytics Job", e);
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    /**
     * Read traffic events from Kafka
     */
    private static Dataset<Row> readTrafficEvents(SparkSession spark, ConfigLoader config) {
        logger.info("Reading traffic events for analytics...");
        
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
            .selectExpr("CAST(value AS STRING) as json_value")
            .select(from_json(col("json_value"), AvroDeserializer.getTrafficReadingSchema()).as("data"))
            .select("data.*");
        
        return parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withWatermark("event_time", "2 minutes")
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
    }
    
    /**
     * Calculate real-time KPIs
     */
    private static Dataset<Row> calculateKPIs(Dataset<Row> events) {
        logger.info("Calculating real-time KPIs...");
        
        // Calculate global metrics over last 5 minutes
        return events
            .groupBy(window(col("event_time"), "5 minutes", "1 minute"))
            .agg(
                // City-wide average speed
                avg("averageSpeed").as("city_avg_speed"),
                
                // Total vehicles on roads
                sum("vehicleCount").as("total_vehicles"),
                
                // Congestion distribution
                count(when(col("congestionLevel").equalTo("FREE_FLOW"), 1)).as("free_flow_roads"),
                count(when(col("congestionLevel").equalTo("SEVERE"), 1)).as("severe_congestion_roads"),
                
                // Active incidents
                sum(when(col("incidentDetected"), 1).otherwise(0)).as("active_incidents"),
                
                // Top congested road segments
                collect_list(
                    when(col("averageSpeed").lt(20),
                        struct(
                            col("roadSegmentId"),
                            col("sensorCode"),
                            col("averageSpeed"),
                            col("congestionLevel"),
                            col("latitude"),
                            col("longitude")
                        )
                    )
                ).as("congested_segments"),
                
                // Active sensors
                countDistinct("sensorId").as("active_sensors"),
                
                // Average occupancy
                avg("occupancy").as("avg_occupancy"),
                
                // Timestamp
                max("event_time").as("last_update")
            )
            .select(
                col("window.start").as("window_start"),
                col("window.end").as("window_end"),
                col("city_avg_speed"),
                col("total_vehicles"),
                col("free_flow_roads"),
                col("severe_congestion_roads"),
                col("active_incidents"),
                col("congested_segments"),
                col("active_sensors"),
                col("avg_occupancy"),
                col("last_update"),
                current_timestamp().as("computed_at")
            );
    }
    
    /**
     * Write KPIs to Redis
     */
    private static StreamingQuery writeToRedis(Dataset<Row> kpis, ConfigLoader config) throws java.util.concurrent.TimeoutException {
        logger.info("Writing KPIs to Redis...");
        
        return kpis
            .writeStream()
            .foreachBatch((batchDF, batchId) -> {
                logger.info("Writing batch {} to Redis", batchId);
                
                // Get Redis connection
                try (JedisPool pool = new JedisPool(config.getRedisHost(), config.getRedisPort())) {
                    try (Jedis jedis = pool.getResource()) {
                        jedis.select(config.getRedisDatabase());
                        
                        // Convert to JSON and store in Redis
                        batchDF.toJSON().collectAsList().forEach(json -> {
                            String key = "cityflow:analytics:realtime";
                            jedis.setex(key, config.getAnalyticsTtl(), json);
                            logger.debug("Stored analytics in Redis: {}", key);
                        });
                        
                        logger.info("Batch {} written to Redis successfully", batchId);
                    }
                }
            })
            .trigger(Trigger.ProcessingTime(config.getTriggerInterval("analytics")))
            .option("checkpointLocation", config.getCheckpointLocation() + "/analytics")
            .queryName("real-time-analytics")
            .start();
    }
}
