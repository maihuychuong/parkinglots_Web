package com.example.demo.entity;

import com.example.demo.model.enums.VehicleType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id
    private String id;

    private String plateNumber;

    private VehicleType vehicleType;

    private LocalDateTime createdAt;
}
