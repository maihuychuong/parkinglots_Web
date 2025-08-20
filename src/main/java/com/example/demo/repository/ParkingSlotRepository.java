package com.example.demo.repository;

import com.example.demo.entity.ParkingSlot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingSlotRepository extends MongoRepository<ParkingSlot, String> {
    long countByIsAvailableTrue();
    List<ParkingSlot> findByLot_Id(String lotId);
}
