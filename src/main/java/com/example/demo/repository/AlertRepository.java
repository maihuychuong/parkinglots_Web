package com.example.demo.repository;

import com.example.demo.entity.Alert;
import com.example.demo.entity.ParkingLog;
import com.example.demo.model.enums.AlertType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findAllByOrderByCreatedAtDesc();
    List<Alert> findByTypeOrderByCreatedAtDesc(AlertType type);
    List<Alert> findByResolvedOrderByCreatedAtDesc(Boolean resolved);
    List<Alert> findByTypeAndResolvedOrderByCreatedAtDesc(AlertType type, Boolean resolved);
    long countByResolvedFalse();
    boolean existsByLogAndTypeAndResolvedFalse(ParkingLog log, AlertType type);

}
