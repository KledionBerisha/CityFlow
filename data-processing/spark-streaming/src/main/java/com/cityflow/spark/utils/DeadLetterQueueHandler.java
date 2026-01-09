package com.cityflow.spark.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;

/**
 * Dead Letter Queue handler for processing failed messages
 */
public class DeadLetterQueueHandler implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);
    private static final long serialVersionUID = 1L;
    
    private final String dlqTopic;
    private final String bootstrapServers;
    
    public DeadLetterQueueHandler(String bootstrapServers, String dlqTopic) {
        this.bootstrapServers = bootstrapServers;
        this.dlqTopic = dlqTopic;
    }
    
    /**
     * Send failed messages to DLQ
     */
    public void sendToDLQ(
            Dataset<Row> failedMessages,
            String originalTopic,
            String errorReason,
            String jobName) {
        
        logger.warn("Sending {} failed messages to DLQ", failedMessages.count());
        
        // Add metadata columns
        Dataset<Row> enrichedMessages = failedMessages
            .withColumn("dlq_timestamp", functions.lit(Instant.now().toString()))
            .withColumn("original_topic", functions.lit(originalTopic))
            .withColumn("error_reason", functions.lit(errorReason))
            .withColumn("job_name", functions.lit(jobName))
            .withColumn("dlq_key", functions.concat(
                functions.lit(originalTopic),
                functions.lit("-"),
                functions.col("key")
            ));
        
        // Write to DLQ topic
        enrichedMessages
            .selectExpr("CAST(dlq_key AS STRING) as key", "to_json(struct(*)) AS value")
            .write()
            .format("kafka")
            .option("kafka.bootstrap.servers", bootstrapServers)
            .option("topic", dlqTopic)
            .mode(SaveMode.Append)
            .save();
        
        logger.info("Successfully sent failed messages to DLQ topic: {}", dlqTopic);
    }
    
    /**
     * Create DLQ error record
     */
    public static ObjectNode createDLQRecord(
            String originalMessage,
            String errorMessage,
            String stackTrace,
            String originalTopic) {
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dlqRecord = mapper.createObjectNode();
        
        dlqRecord.put("timestamp", Instant.now().toString());
        dlqRecord.put("original_topic", originalTopic);
        dlqRecord.put("original_message", originalMessage);
        dlqRecord.put("error_message", errorMessage);
        dlqRecord.put("stack_trace", stackTrace);
        
        return dlqRecord;
    }
    
    /**
     * Filter valid and invalid messages
     */
    public static class ValidationResult {
        public final Dataset<Row> validMessages;
        public final Dataset<Row> invalidMessages;
        
        public ValidationResult(Dataset<Row> validMessages, Dataset<Row> invalidMessages) {
            this.validMessages = validMessages;
            this.invalidMessages = invalidMessages;
        }
    }
    
    /**
     * Validate messages and separate valid from invalid
     */
    public ValidationResult validateMessages(Dataset<Row> messages, String validationColumn) {
        Dataset<Row> validMessages = messages.filter(functions.col(validationColumn).isNotNull());
        Dataset<Row> invalidMessages = messages.filter(functions.col(validationColumn).isNull());
        
        long validCount = validMessages.count();
        long invalidCount = invalidMessages.count();
        
        logger.info("Validation results - Valid: {}, Invalid: {}", validCount, invalidCount);
        
        return new ValidationResult(validMessages, invalidMessages);
    }
}
