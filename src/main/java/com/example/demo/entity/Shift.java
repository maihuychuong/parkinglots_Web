package com.example.demo.entity;

import com.example.demo.model.enums.ShiftType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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
    @NotNull(message = "Nhân viên không được để trống")
    private User user;

    @NotNull(message = "Ngày ca không được để trống")
    private LocalDate shiftDate;

    @NotNull(message = "Loại ca không được để trống")
    private ShiftType shiftType;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime shiftStart;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime shiftEnd;

    private String note;

    // Transient field for display
    @Transient
    private String employeeName;
}