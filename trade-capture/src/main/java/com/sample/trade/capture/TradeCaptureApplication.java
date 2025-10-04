package com.sample.trade.capture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sample.trade.common.store.TradeStore;
import com.sample.trade.validationstorage.service.TradeProjectionService;
import com.sample.trade.validationstorage.service.TradeProjectionServiceImpl;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.JDBCType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TradeCaptureApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeCaptureApplication.class, args);
    }

    @Value("${amazon.dynamodb.endpoint}")
    private String dynamoDbEndpoint;

    @Value("${amazon.aws.accesskey}")
    private String awsAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String awsSecretKey;

    @Value("${amazon.aws.region}")
    private String awsRegion;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        System.out.println("Configuring DynamoDB with:");
        System.out.println("Endpoint: " + dynamoDbEndpoint);
        System.out.println("Region: " + awsRegion);
        System.out.println("Access Key: " + (awsAccessKey != null ? awsAccessKey.substring(0, 8) + "..." : "null"));

        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dynamoDbEndpoint, awsRegion))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .build();
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        // Configure DynamoDBMapper with table name override
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build();
        return new DynamoDBMapper(amazonDynamoDB, mapperConfig);
    }

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String databaseDriverClassName;

    @Bean
    public JdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(databaseDriverClassName);
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(databaseUsername);
        dataSource.setPassword(databasePassword);
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TradeStore tradeStore(JdbcTemplate jdbcTemplate) {
        return new TradeStore(jdbcTemplate);
    }

    @Bean
    public TradeProjectionService tradeProjectionService(TradeStore tradeStore) {
        return new TradeProjectionServiceImpl(tradeStore);
    }
}
