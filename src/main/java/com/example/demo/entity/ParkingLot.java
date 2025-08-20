package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;

@Document(collection = "parking_lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {
    @Id
    private String id;

    private String name;

    private String address;

    private String phone;

    private LocalTime openTime;

    private LocalTime closeTime;
}
