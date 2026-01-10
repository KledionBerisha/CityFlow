package com.cityflow.spark.utils;

import io.delta.tables.DeltaTable;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for writing data to Delta Lake with ACID guarantees
 */
public class DeltaLakeWriter implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaLakeWriter.class);
    private static final long serialVersionUID = 1L;
    
    private final SparkSession spark;
    
    public DeltaLakeWriter(SparkSession spark) {
        this.spark = spark;
    }
    
    /**
     * Write streaming data to Delta Lake
     */
    public StreamingQuery writeStream(
            Dataset<Row> df,
            String deltaPath,
            String checkpointPath,
            String queryName,
            String triggerInterval,
            String... partitionColumns) throws java.util.concurrent.TimeoutException {
        
        logger.info("Starting Delta Lake streaming write to: {}", deltaPath);
        logger.info("Checkpoint location: {}", checkpointPath);
        logger.info("Query name: {}", queryName);
        
        var writeStream = df.writeStream()
            .format("delta")
            .outputMode("append")
            .option("checkpointLocation", checkpointPath)
            .option("mergeSchema", "true")
            .trigger(Trigger.ProcessingTime(triggerInterval));
        
        // Add partitioning if specified
        if (partitionColumns != null && partitionColumns.length > 0) {
            logger.info("Partitioning by: {}", String.join(", ", partitionColumns));
            writeStream = writeStream.partitionBy(partitionColumns);
        }
        
        StreamingQuery query = writeStream
            .queryName(queryName)
            .start(deltaPath);
        
        logger.info("Streaming query started: {}", query.name());
        return query;
    }
    
    /**
     * Write batch data to Delta Lake
     */
    public void writeBatch(
            Dataset<Row> df,
            String deltaPath,
            SaveMode saveMode,
            String... partitionColumns) {
        
        logger.info("Writing batch data to Delta Lake: {}", deltaPath);
        logger.info("Save mode: {}", saveMode);
        
        var writer = df.write()
            .format("delta")
            .mode(saveMode)
            .option("mergeSchema", "true");
        
        if (partitionColumns != null && partitionColumns.length > 0) {
            logger.info("Partitioning by: {}", String.join(", ", partitionColumns));
            writer = writer.partitionBy(partitionColumns);
        }
        
        writer.save(deltaPath);
        logger.info("Batch write completed successfully");
    }
    
    /**
     * Merge (upsert) data into Delta Lake
     */
    public void merge(
            Dataset<Row> sourceDF,
            String deltaPath,
            String mergeCondition,
            String... updateColumns) {
        
        logger.info("Performing Delta Lake merge operation");
        logger.info("Target path: {}", deltaPath);
        logger.info("Merge condition: {}", mergeCondition);
        
        DeltaTable targetTable = DeltaTable.forPath(spark, deltaPath);
        
        var mergeBuilder = targetTable
            .as("target")
            .merge(sourceDF.as("source"), mergeCondition);
        
        // Update matched records
        var updateMap = new java.util.HashMap<String, String>();
        for (String column : updateColumns) {
            updateMap.put(column, "source." + column);
        }
        
        mergeBuilder
            .whenMatched()
            .updateExpr(updateMap)
            .whenNotMatched()
            .insertAll()
            .execute();
        
        logger.info("Merge operation completed successfully");
    }
    
    /**
     * Optimize Delta Lake table (compaction)
     */
    public void optimize(String deltaPath, String... zOrderColumns) {
        logger.info("Optimizing Delta Lake table: {}", deltaPath);
        
        DeltaTable table = DeltaTable.forPath(spark, deltaPath);
        
        if (zOrderColumns != null && zOrderColumns.length > 0) {
            logger.info("Z-ordering by: {}", String.join(", ", zOrderColumns));
            table.optimize().executeZOrderBy(zOrderColumns);
        } else {
            table.optimize().executeCompaction();
        }
        
        logger.info("Optimization completed successfully");
    }
    
    /**
     * Vacuum old files from Delta Lake
     */
    public void vacuum(String deltaPath, int retentionHours) {
        logger.info("Vacuuming Delta Lake table: {}", deltaPath);
        logger.info("Retention hours: {}", retentionHours);
        
        DeltaTable table = DeltaTable.forPath(spark, deltaPath);
        table.vacuum(retentionHours);
        
        logger.info("Vacuum completed successfully");
    }
    
    /**
     * Read from Delta Lake
     */
    public Dataset<Row> read(String deltaPath) {
        logger.info("Reading from Delta Lake: {}", deltaPath);
        return spark.read().format("delta").load(deltaPath);
    }
    
    /**
     * Read from Delta Lake with time travel
     */
    public Dataset<Row> readVersion(String deltaPath, long version) {
        logger.info("Reading Delta Lake version {} from: {}", version, deltaPath);
        return spark.read()
            .format("delta")
            .option("versionAsOf", version)
            .load(deltaPath);
    }
    
    /**
     * Get Delta Lake table history
     */
    public Dataset<Row> getHistory(String deltaPath) {
        DeltaTable table = DeltaTable.forPath(spark, deltaPath);
        return table.history();
    }
}
