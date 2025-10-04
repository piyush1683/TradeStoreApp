package com.sample.trade.validationstorage;

import com.sample.trade.common.store.TradeStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class TradeValidationStorageApplication {

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/tradestorepgdb}")
    private String databaseUrl;

    @Value("${spring.datasource.username:postgres}")
    private String databaseUsername;

    @Value("${spring.datasource.password:password}")
    private String databasePassword;

    public static void main(String[] args) {
        SpringApplication.run(TradeValidationStorageApplication.class, args);
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(databaseUsername);
        dataSource.setPassword(databasePassword);
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TradeStore tradeStore(JdbcTemplate jdbcTemplate) {
        return new TradeStore(jdbcTemplate);
    }
}
