package com.example.demo.service;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender javaMailSender;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository, JavaMailSender javaMailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.javaMailSender = javaMailSender;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    public Optional<User> findByUsernameSafe(String username) {
        return userRepository.findByUsername(username);
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    public User saveUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public void createPasswordResetToken(User user, String token) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserEmail(user.getEmail());
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(resetToken);
    }

    public boolean isValidToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        return resetToken != null && resetToken.getExpiryDate().isAfter(LocalDateTime.now());
    }

    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        Optional<User> optionalUser = userRepository.findByEmail(resetToken.getUserEmail());
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        return true;
    }

    public boolean sendPasswordResetEmail(User user, String appUrl, String token) {
        try {
            String resetUrl = appUrl + "/reset-password?token=" + token;

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("Yêu cầu đặt lại mật khẩu");
            mailMessage.setText("Chào " + user.getUsername() + ",\n\n"
                    + "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng nhấp vào liên kết để đặt lại:\n"
                    + resetUrl + "\n\n"
                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");

            javaMailSender.send(mailMessage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}