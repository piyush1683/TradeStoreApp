package com.sample.trade.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.trade.common.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class TradeIngestionController {

    private final KafkaTemplate<String, Trade> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TradeIngestionController(KafkaTemplate<String, Trade> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/trade")
    public ResponseEntity<String> acceptTrade(@RequestBody Trade trade) {
        try {
            // Validate trade
            if (trade.getTradeId() == null || trade.getTradeId().trim().isEmpty() ||
                    trade.getCounterPartyId() == null || trade.getCounterPartyId().trim().isEmpty() ||
                    trade.getBookId() == null || trade.getBookId().trim().isEmpty() ||
                    trade.getMaturityDate() == null || trade.getCreatedDate() == null ||
                    trade.getExpired() == null || trade.getExpired().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid trade data - missing required fields");
            }

            // Publish to Kafka
            CompletableFuture<SendResult<String, Trade>> future = kafkaTemplate.send("trade_ingestion",
                    trade.getTradeId(), trade);
            future.get(5, java.util.concurrent.TimeUnit.SECONDS);

            return ResponseEntity.accepted().body("Trade accepted and published to Kafka");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Failed to publish trade to Kafka: " + e.getMessage());
        }
    }

    @PostMapping("/trades/upload")
    public ResponseEntity<String> uploadTrades(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse CSV file
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            int processedCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                try {
                    Trade trade = parseCsvLine(line);
                    // Publish to Kafka
                    CompletableFuture<SendResult<String, Trade>> future = kafkaTemplate.send("trade_ingestion",
                            trade.getTradeId(), trade);
                    future.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    processedCount++;
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Invalid CSV format: " + e.getMessage());
                }
            }

            return ResponseEntity.accepted().body("Successfully processed " + processedCount + " trades");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to process file: " + e.getMessage());
        }
    }

    private Trade parseCsvLine(String line) throws Exception {
        String[] fields = line.split(",");
        if (fields.length != 7) {
            throw new IllegalArgumentException("Invalid CSV format - expected 7 fields");
        }

        Trade trade = new Trade();
        trade.setTradeId(fields[0].trim());
        trade.setVersion(Integer.parseInt(fields[1].trim()));
        trade.setCounterPartyId(fields[2].trim());
        trade.setBookId(fields[3].trim());

        // Parse maturity date
        String maturityDateStr = fields[4].trim();
        if ("<today date>".equals(maturityDateStr)) {
            trade.setMaturityDate(LocalDate.now());
        } else {
            trade.setMaturityDate(LocalDate.parse(maturityDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        // Parse created date
        String createdDateStr = fields[5].trim();
        if ("<today date>".equals(createdDateStr)) {
            trade.setCreatedDate(LocalDate.now());
        } else {
            trade.setCreatedDate(LocalDate.parse(createdDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        trade.setExpired(fields[6].trim());

        return trade;
    }
}
