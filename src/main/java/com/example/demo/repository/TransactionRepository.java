package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import com.example.demo.model.enums.TransactionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByStatus(TransactionStatus status);
    long countByPaidAtBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
