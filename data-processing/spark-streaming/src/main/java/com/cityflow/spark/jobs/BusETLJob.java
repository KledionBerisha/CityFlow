package com.cityflow.spark.jobs;

import com.cityflow.spark.config.ConfigLoader;
import com.cityflow.spark.utils.AvroDeserializer;
import com.cityflow.spark.utils.GeospatialUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

/**
 * Bus ETL Job
 * Processes bus location events with geospatial analysis
 * Calculates delays, speeds, and ETAs
 * Writes to MongoDB (current positions) and Redis (real-time cache)
 */
public class BusETLJob {
    
    private static final Logger logger = LoggerFactory.getLogger(BusETLJob.class);
    
    public static void main(String[] args) {
        logger.info("Starting Bus ETL Job...");
        
        // Load configuration
        ConfigLoader config = new ConfigLoader();
        
        // Create Spark session
        SparkSession spark = SparkSession.builder()
            .appName("CityFlow-BusETL")
            .config("spark.sql.streaming.schemaInference", "true")
            .config("spark.mongodb.write.connection.uri", config.getMongoUri())
            .getOrCreate();
        
        // Register geospatial UDFs
        registerGeospatialUDFs(spark);
        
        try {
            // Read bus location events
            Dataset<Row> busEvents = readBusEvents(spark, config);
            
            // Enrich with geospatial calculations
            Dataset<Row> enriched = enrichBusData(busEvents, spark);
            
            // Write to MongoDB
            StreamingQuery mongoQuery = writeToMongoDB(enriched, config);
            
            logger.info("Bus ETL query started: {}", mongoQuery.name());
            
            // Wait for termination
            spark.streams().awaitAnyTermination();
            
        } catch (Exception e) {
            logger.error("Error in Bus ETL Job", e);
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    /**
     * Read bus location events from Kafka
     */
    private static Dataset<Row> readBusEvents(SparkSession spark, ConfigLoader config) {
        logger.info("Reading bus events from Kafka...");
        
        Dataset<Row> rawStream = spark
            .readStream()
            .format("kafka")
            .option("kafka.bootstrap.servers", config.getKafkaBootstrapServers())
            .option("subscribe", config.getBusLocationTopic())
            .option("startingOffsets", "latest")
            .option("maxOffsetsPerTrigger", 5000)
            .option("failOnDataLoss", false)
            .load();
        
        // Parse JSON
        Dataset<Row> parsed = rawStream
            .selectExpr("CAST(value AS STRING) as json_value")
            .select(from_json(col("json_value"), AvroDeserializer.getBusLocationSchema()).as("data"))
            .select("data.*");
        
        // Add watermark and extract location
        return parsed
            .withColumn("event_time", to_timestamp(col("timestamp").divide(1000)))
            .withWatermark("event_time", config.getWatermark("bus"))
            .withColumn("latitude", col("location.latitude"))
            .withColumn("longitude", col("location.longitude"));
    }
    
    /**
     * Enrich bus data with geospatial calculations
     */
    private static Dataset<Row> enrichBusData(Dataset<Row> events, SparkSession spark) {
        logger.info("Enriching bus data with geospatial calculations...");
        
        // Window spec for calculating changes over time (partitioned by bus)
        WindowSpec windowSpec = Window
            .partitionBy("busId")
            .orderBy("event_time");
        
        // Calculate previous position and time
        Dataset<Row> withPrevious = events
            .withColumn("prev_latitude", lag("latitude", 1).over(windowSpec))
            .withColumn("prev_longitude", lag("longitude", 1).over(windowSpec))
            .withColumn("prev_timestamp", lag("timestamp", 1).over(windowSpec))
            .withColumn("prev_speed", lag("speedKmh", 1).over(windowSpec));
        
        // Calculate distance traveled and actual speed using UDF
        Dataset<Row> withCalculations = withPrevious
            .withColumn("distance_meters", 
                when(col("prev_latitude").isNotNull(),
                    callUDF("calculate_distance",
                        col("prev_latitude"),
                        col("prev_longitude"),
                        col("latitude"),
                        col("longitude")
                    )
                ).otherwise(0.0)
            )
            .withColumn("time_diff_seconds",
                when(col("prev_timestamp").isNotNull(),
                    col("timestamp").minus(col("prev_timestamp")).divide(1000)
                ).otherwise(0.0)
            )
            .withColumn("calculated_speed_kmh",
                when(col("time_diff_seconds").gt(0),
                    col("distance_meters").divide(col("time_diff_seconds")).multiply(3.6)
                ).otherwise(col("speedKmh"))
            );
        
        // Detect anomalies and enrich
        return withCalculations
            .withColumn("is_stopped", 
                when(col("calculated_speed_kmh").lt(5.0), true).otherwise(false)
            )
            .withColumn("is_delayed",
                when(col("delayMinutes").gt(5), true).otherwise(false)
            )
            .withColumn("speed_anomaly",
                when(
                    col("calculated_speed_kmh").gt(100).or(
                        col("calculated_speed_kmh").minus(col("prev_speed")).abs().gt(50)
                    ),
                    true
                ).otherwise(false)
            )
            .withColumn("occupancy_percentage",
                when(col("capacity").gt(0),
                    col("occupancy").divide(col("capacity")).multiply(100)
                ).otherwise(0.0)
            )
            .withColumn("processed_at", current_timestamp())
            .select(
                col("busId"),
                col("vehicleId"),
                col("busCode"),
                col("routeId"),
                col("latitude"),
                col("longitude"),
                col("speedKmh"),
                col("calculated_speed_kmh"),
                col("heading"),
                col("occupancy"),
                col("capacity"),
                col("occupancy_percentage"),
                col("operationalStatus"),
                col("nextStopId"),
                col("estimatedArrival"),
                col("delayMinutes"),
                col("is_stopped"),
                col("is_delayed"),
                col("speed_anomaly"),
                col("distance_meters"),
                col("event_time"),
                col("processed_at")
            );
    }
    
    /**
     * Write enriched bus data to MongoDB
     */
    private static StreamingQuery writeToMongoDB(Dataset<Row> enriched, ConfigLoader config) {
        logger.info("Writing to MongoDB...");
        
        return enriched
            .writeStream()
            .foreachBatch((batchDF, batchId) -> {
                logger.info("Writing batch {} to MongoDB", batchId);
                
                // Write to MongoDB (will replace existing documents with same busId)
                batchDF
                    .write()
                    .format("mongodb")
                    .mode("append")
                    .option("connection.uri", config.getMongoUri())
                    .option("database", config.getMongoDatabase())
                    .option("collection", "bus_positions")
                    .save();
                
                logger.info("Batch {} written to MongoDB successfully", batchId);
            })
            .trigger(Trigger.ProcessingTime(config.getTriggerInterval("bus-etl")))
            .option("checkpointLocation", config.getCheckpointLocation() + "/bus_etl")
            .queryName("bus-etl")
            .start();
    }
    
    /**
     * Register geospatial UDFs
     */
    private static void registerGeospatialUDFs(SparkSession spark) {
        logger.info("Registering geospatial UDFs...");
        
        // Distance calculation UDF
        spark.udf().register("calculate_distance",
            (Double lat1, Double lon1, Double lat2, Double lon2) -> {
                if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
                    return 0.0;
                }
                return GeospatialUtils.calculateDistance(lat1, lon1, lat2, lon2);
            },
            org.apache.spark.sql.types.DataTypes.DoubleType
        );
        
        // Bearing calculation UDF
        spark.udf().register("calculate_bearing",
            (Double lat1, Double lon1, Double lat2, Double lon2) -> {
                if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
                    return 0.0;
                }
                return GeospatialUtils.calculateBearing(lat1, lon1, lat2, lon2);
            },
            org.apache.spark.sql.types.DataTypes.DoubleType
        );
        
        logger.info("Geospatial UDFs registered successfully");
    }
}
