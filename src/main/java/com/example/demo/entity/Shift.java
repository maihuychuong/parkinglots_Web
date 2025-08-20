package com.example.demo.entity;

import com.example.demo.model.enums.ShiftType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

@Document(collection = "shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {
    @Id
    private String id;

    @DBRef
    private User user;

    private LocalDate shiftDate;

    private ShiftType shiftType;

    private LocalTime shiftStart;

    private LocalTime shiftEnd;

    private String note;
}
