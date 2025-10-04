package com.sample.trade.ingestion.api;

import com.sample.trade.common.model.Trade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

@RestController
@RequestMapping("/api")
public class TradeIngestionServiceImp implements TradeIngestionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KafkaTradeIngestionService kafkaTradeIngestionService;

    public TradeIngestionServiceImp(KafkaTradeIngestionService kafkaTradeIngestionService) {
        this.kafkaTradeIngestionService = kafkaTradeIngestionService;
    }

    @Override
    public void acceptTrade(Trade trade) {
        try {
            kafkaTradeIngestionService.sendTradetoKafka(trade);
        } catch (Exception e) {
            throw new TradeIngestionException("Failed to publish trade to Kafka", e);
        }
    }

    @Override
    public void acceptTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            acceptTrade(trade);
        }
    }

    @PostMapping("/trades")
    public ResponseEntity<Void> createTrades(@RequestBody List<Trade> trades) {
        acceptTrades(trades);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping("/trade")
    public ResponseEntity<Void> createTrade(@RequestBody Trade trade) {
        acceptTrade(trade);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/trades/upload", consumes = { "multipart/form-data" })
    public ResponseEntity<Void> uploadTradesCsv(MultipartFile file) throws IOException {
        List<Trade> trades = parseCsv(file);
        acceptTrades(trades);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
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
