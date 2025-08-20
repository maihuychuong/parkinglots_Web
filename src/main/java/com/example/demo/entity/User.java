package com.example.demo.entity;

import com.example.demo.model.enums.Role;
import com.example.demo.model.enums.UserStatus;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Indexed(unique = true, sparse = true)
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
}
