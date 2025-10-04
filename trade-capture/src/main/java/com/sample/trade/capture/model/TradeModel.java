package com.sample.trade.capture.model;

import java.time.LocalDate;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "trades-event-store")
public class TradeModel {

    @DynamoDBHashKey
    public String reqtradeid;

    @DynamoDBAttribute
    public String tradeId;
    @DynamoDBAttribute
    public int version;
    @DynamoDBAttribute
    public String counterPartyId;
    @DynamoDBAttribute
    public String bookId;
    @DynamoDBAttribute
    public LocalDate maturityDate;
    @DynamoDBAttribute
    public LocalDate createdDate;
    @DynamoDBAttribute
    public String expired;

}
