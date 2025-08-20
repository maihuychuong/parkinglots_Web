package com.example.demo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {
    @Id
    private String id;

    private String slotCode;

    private Boolean isAvailable = true;

    @DBRef
    private ParkingLot lot;

    private String note;
}
