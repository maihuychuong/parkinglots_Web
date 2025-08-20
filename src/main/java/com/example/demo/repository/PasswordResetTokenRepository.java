package com.example.demo.repository;

import com.example.demo.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    PasswordResetToken findByToken(String token);
}
