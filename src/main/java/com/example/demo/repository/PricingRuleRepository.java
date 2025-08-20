package com.example.demo.repository;

import com.example.demo.entity.PricingRule;
import com.example.demo.model.enums.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PricingRuleRepository extends MongoRepository<PricingRule, String> {
    Optional<PricingRule> findByVehicleType(VehicleType vehicleType);
}
