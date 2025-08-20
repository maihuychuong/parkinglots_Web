package com.example.demo.repository;

import com.example.demo.entity.ParkingLog;
import com.example.demo.entity.ParkingLot;
import com.example.demo.model.enums.LogStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ParkingLotRepository extends MongoRepository<ParkingLot, String> {
    List<ParkingLot> findAllByOrderByNameAsc();
    List<ParkingLot> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

}
