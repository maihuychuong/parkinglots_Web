package com.example.demo.service;

import com.example.demo.entity.ParkingLot;
import com.example.demo.repository.ParkingLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingLotService {
    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotService(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    public List<ParkingLot> findAll() {
        return parkingLotRepository.findAll();
    }

    public List<ParkingLot> getAllParkingLots(String name) {
        if (name == null || name.trim().isEmpty()) {
            return parkingLotRepository.findAllByOrderByNameAsc();
        }
        return parkingLotRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name);
    }
}