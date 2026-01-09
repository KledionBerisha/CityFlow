package com.cityflow.spark.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Configuration loader for Spark jobs
 * Loads application.conf and provides type-safe access to configuration values
 */
public class ConfigLoader implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final long serialVersionUID = 1L;
    
    private final Config config;
    
    public ConfigLoader() {
        this.config = ConfigFactory.load();
        logger.info("Configuration loaded successfully");
    }
    
    public ConfigLoader(String configFile) {
        this.config = ConfigFactory.load(configFile);
        logger.info("Configuration loaded from: {}", configFile);
    }
    
    // Kafka Configuration
    public String getKafkaBootstrapServers() {
        return config.getString("cityflow.kafka.bootstrap-servers");
    }
    
    public String getSchemaRegistryUrl() {
        return config.getString("cityflow.kafka.schema-registry-url");
    }
    
    public String getTrafficReadingTopic() {
        return config.getString("cityflow.kafka.topics.traffic-reading");
    }
    
    public String getBusLocationTopic() {
        return config.getString("cityflow.kafka.topics.bus-location");
    }
    
    public String getIncidentTopic() {
        return config.getString("cityflow.kafka.topics.incident");
    }
    
    public String getSensorStatusTopic() {
        return config.getString("cityflow.kafka.topics.sensor-status");
    }
    
    public String getBusStatusTopic() {
        return config.getString("cityflow.kafka.topics.bus-status");
    }
    
    public String getEnrichedIncidentTopic() {
        return config.getString("cityflow.kafka.topics.enriched-incident");
    }
    
    public String getDeadLetterQueueTopic() {
        return config.getString("cityflow.kafka.topics.dead-letter-queue");
    }
    
    public String getConsumerGroupId(String jobName) {
        return config.getString("cityflow.kafka.consumer.group-id-prefix") + "-" + jobName;
    }
    
    // PostgreSQL Configuration
    public String getPostgresUrl() {
        return config.getString("cityflow.postgresql.url");
    }
    
    public String getPostgresUser() {
        return config.getString("cityflow.postgresql.user");
    }
    
    public String getPostgresPassword() {
        return config.getString("cityflow.postgresql.password");
    }
    
    public String getPostgresDriver() {
        return config.getString("cityflow.postgresql.driver");
    }
    
    // MongoDB Configuration
    public String getMongoUri() {
        return config.getString("cityflow.mongodb.uri");
    }
    
    public String getMongoDatabase() {
        return config.getString("cityflow.mongodb.database");
    }
    
    // Redis Configuration
    public String getRedisHost() {
        return config.getString("cityflow.redis.host");
    }
    
    public int getRedisPort() {
        return config.getInt("cityflow.redis.port");
    }
    
    public int getRedisDatabase() {
        return config.getInt("cityflow.redis.database");
    }
    
    public int getTrafficCacheTtl() {
        return config.getInt("cityflow.redis.ttl.traffic-cache");
    }
    
    public int getBusPositionTtl() {
        return config.getInt("cityflow.redis.ttl.bus-position");
    }
    
    public int getAnalyticsTtl() {
        return config.getInt("cityflow.redis.ttl.analytics");
    }
    
    // Delta Lake Configuration
    public String getDeltaLakeBasePath() {
        return config.getString("cityflow.delta-lake.base-path");
    }
    
    public String getCheckpointLocation() {
        return config.getString("cityflow.delta-lake.checkpoint-location");
    }
    
    public String getTrafficDeltaPath() {
        return getDeltaLakeBasePath() + "/traffic";
    }
    
    public String getBusDeltaPath() {
        return getDeltaLakeBasePath() + "/bus";
    }
    
    public String getIncidentDeltaPath() {
        return getDeltaLakeBasePath() + "/incident";
    }
    
    // Streaming Configuration
    public String getTriggerInterval(String jobType) {
        return config.getString("cityflow.streaming.trigger." + jobType);
    }
    
    public String getWatermark(String eventType) {
        return config.getString("cityflow.streaming.watermark." + eventType);
    }
    
    public String getShortWindow() {
        return config.getString("cityflow.streaming.windows.short");
    }
    
    public String getMediumWindow() {
        return config.getString("cityflow.streaming.windows.medium");
    }
    
    public String getLongWindow() {
        return config.getString("cityflow.streaming.windows.long");
    }
    
    // Data Quality Configuration
    public boolean isDataQualityEnabled() {
        return config.getBoolean("cityflow.data-quality.enabled");
    }
    
    public boolean isDlqEnabled() {
        return config.getBoolean("cityflow.data-quality.dlq.enabled");
    }
    
    public double getNullThreshold() {
        return config.getDouble("cityflow.data-quality.validation.null-threshold");
    }
    
    // Geospatial Configuration
    public int getCoordinatePrecision() {
        return config.getInt("cityflow.geospatial.coordinate-precision");
    }
    
    public double getDistanceThreshold() {
        return config.getDouble("cityflow.geospatial.distance-threshold");
    }
    
    // Logging
    public String getLogLevel() {
        return config.getString("cityflow.logging.level");
    }
    
    public String getMetricsInterval() {
        return config.getString("cityflow.logging.metrics-interval");
    }
    
    // Generic config access
    public Config getConfig() {
        return config;
    }
    
    public String getString(String path) {
        return config.getString(path);
    }
    
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
}
