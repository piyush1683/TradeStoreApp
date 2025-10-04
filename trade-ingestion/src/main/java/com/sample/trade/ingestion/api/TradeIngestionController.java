package com.sample.trade.ingestion.api;

import com.sample.trade.common.model.Trade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.sample.trade.ingestion.service.KafkaTradeIngestionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TradeIngestionController implements TradeIngestionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KafkaTradeIngestionService kafkaTradeIngestionService;

    public TradeIngestionController(KafkaTradeIngestionService kafkaTradeIngestionService) {
        this.kafkaTradeIngestionService = kafkaTradeIngestionService;
    }

    @Override
    public void acceptTrade(Trade trade, String requestId) {
        try {
            trade.setRequestId(requestId);
            kafkaTradeIngestionService.sendTradetoKafka(trade);
        } catch (Exception e) {
            throw new TradeIngestionException("Failed to publish trade to Kafka", e);
        }
    }

    @Override
    public void acceptTrades(List<Trade> trades, String requestId) {
        for (Trade trade : trades) {
            // trade.setRequestId(requestId);
            acceptTrade(trade, requestId);
        }
    }

    @PostMapping("/trades")
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> createTrades(@RequestBody List<Trade> trades) {
        String requestId = UUID.randomUUID().toString();
        acceptTrades(trades, requestId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Trade accepted with requestId: " + requestId);
    }

    @PostMapping("/trade")
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> createTrade(@RequestBody Trade trade) {
        String requestId = UUID.randomUUID().toString();
        acceptTrade(trade, requestId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Trade accepted with requestId: " + requestId);
    }

    @PostMapping(value = "/trades-file/upload")
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> uploadTradesCsv(MultipartFile file) throws IOException {
        List<Trade> trades = parseCsv(file);
        String requestId = UUID.randomUUID().toString();
        acceptTrades(trades, requestId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Trade accepted with requestId: " + requestId);
    }

    private static List<Trade> parseCsv(MultipartFile file) throws IOException {
        List<Trade> trades = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 7) {
                    continue;
                }
                Trade t = new Trade();
                t.setTradeId(parts[0].trim());
                t.setVersion(Integer.parseInt(parts[1].trim()));
                t.setCounterPartyId(parts[2].trim());
                t.setBookId(parts[3].trim());
                t.setMaturityDate(LocalDate.parse(parts[4].trim(), DATE_FORMAT));
                t.setCreatedDate("<today date>".equalsIgnoreCase(parts[5].trim()) ? LocalDate.now()
                        : LocalDate.parse(parts[5].trim(), DATE_FORMAT));
                t.setExpired(parts[6].trim());
                trades.add(t);
            }
        }
        return trades;
    }
}
