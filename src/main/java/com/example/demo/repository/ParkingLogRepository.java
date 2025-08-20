package com.example.demo.repository;

import com.example.demo.entity.ParkingLog;
import com.example.demo.model.enums.LogStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ParkingLogRepository extends MongoRepository<ParkingLog, String> {
    long countByStatus(LogStatus status);
    List<ParkingLog> findByStatus(LogStatus status);
    List<ParkingLog> findByTimeInBetween(LocalDateTime start, LocalDateTime end);
}
