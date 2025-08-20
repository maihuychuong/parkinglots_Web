package com.example.demo.entity;

import com.example.demo.model.enums.VehicleType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Document(collection = "pricing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {
    @Id
    private String id;

    private VehicleType vehicleType;

    private BigDecimal firstBlockPrice;

    private BigDecimal nextBlockPrice;

    private Integer blockDurationMinutes = 60;

    private LocalDateTime createdAt;
}
