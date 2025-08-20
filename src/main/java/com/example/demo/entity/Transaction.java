package com.example.demo.entity;

import com.example.demo.model.enums.TransactionMethod;
import com.example.demo.model.enums.TransactionStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    private String id;

    @DBRef
    private ParkingLog log;

    private BigDecimal amount;

    private LocalDateTime paidAt;

    private TransactionMethod method;

    private TransactionStatus status;

    private String note;
}
