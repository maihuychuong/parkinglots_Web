package com.example.demo.entity;

import com.example.demo.model.enums.LogStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "parking_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLog {
    @Id
    private String id;

    @DBRef
    private Vehicle vehicle;

    @DBRef
    private ParkingSlot slot;

    @DBRef
    private User staff;

    private Instant timeIn;

    private Instant timeOut;

    private BigDecimal fee;

    private LogStatus status;

    private String note;
}
