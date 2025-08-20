package com.example.demo.repository;

import com.example.demo.entity.Shift;
import com.example.demo.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ShiftRepository extends MongoRepository<Shift, String> {
    List<Shift> findByShiftDate(LocalDate date);
    List<Shift> findByUserOrderByShiftDateDesc(User user);
    List<Shift> findByUserAndShiftDate(User user, LocalDate date);
}
