package com.example.demo.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParkingLogDTO {
    private String id;
    private String plateNumber;
    private String vehicleType;
    private String slotCode;
    private String staffName;
    private String timeIn;
    private String timeOut;
    private BigDecimal fee;
    private String status;
    private String note;
}


