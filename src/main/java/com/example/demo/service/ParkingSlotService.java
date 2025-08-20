package com.example.demo.service;

import com.example.demo.entity.ParkingSlot;
import com.example.demo.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSlotService {
    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingSlotService(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    public List<ParkingSlot> findAll() {
        return parkingSlotRepository.findAll();
    }

    public List<ParkingSlot> findByLotId(String lotId) {
        return parkingSlotRepository.findByLot_Id(lotId);
    }
}