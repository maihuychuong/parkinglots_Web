package com.example.demo.entity;

import com.example.demo.model.enums.AlertType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    private String id;

    private AlertType type;

    private String message;

    @DBRef
    private ParkingLog log;

    private LocalDateTime createdAt;

    private Boolean resolved = false;

    private LocalDateTime resolvedAt;

    @DBRef
    private User resolvedBy; // Nhân viên nào đã xử lý
}

