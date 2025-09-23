package com.example.demo.service;

import com.example.demo.entity.ParkingSlot;
import com.example.demo.repository.ParkingSlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public ParkingSlot save(ParkingSlot parkingSlot) {
        return parkingSlotRepository.save(parkingSlot);
    }

    public void deleteById(String id) {
        parkingSlotRepository.deleteById(id);
    }

    public Optional<ParkingSlot> findById(String id) {
        return parkingSlotRepository.findById(id);
    }
}