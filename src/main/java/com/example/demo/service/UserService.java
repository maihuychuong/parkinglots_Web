package com.example.demo.service;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import com.example.demo.model.enums.Role;
import com.example.demo.model.enums.UserStatus;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender javaMailSender;
    private final Validator validator;
    private final NotificationService notificationService;

    // Username pattern: 4-20 alphanumeric characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,20}$");
    // Password pattern: 6+ characters, at least one letter and one number
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       JavaMailSender javaMailSender, Validator validator,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.javaMailSender = javaMailSender;
        this.validator = validator;
        this.notificationService = notificationService;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tên: " + username));
    }

    public Optional<User> findByUsernameSafe(String username) {
        return userRepository.findByUsername(username);
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        // Validate using Jakarta Validator
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new RuntimeException("Dữ liệu không hợp lệ: " + violations.iterator().next().getMessage());
        }

        // Additional validations
        if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            throw new RuntimeException("Tên người dùng phải từ 4-20 ký tự, chỉ chứa chữ cái và số");
        }
        if (user.getId() == null && (user.getPassword() == null || !PASSWORD_PATTERN.matcher(user.getPassword()).matches())) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự, chứa ít nhất một chữ cái và một số");
        }
        if (user.getRole() == null) {
            throw new RuntimeException("Vai trò không được để trống");
        }
        if (user.getStatus() == null) {
            throw new RuntimeException("Trạng thái không được để trống");
        }

        // Check for duplicate username and email
        boolean isNewUser = user.getId() == null;
        if (isNewUser) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new RuntimeException("Tên người dùng đã tồn tại: " + user.getUsername());
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("Email đã tồn tại: " + user.getEmail());
            }
            // Encode password for new users
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // Set default values
            user.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            if (user.getStatus() == null) {
                user.setStatus(UserStatus.ACTIVE);
            }
            if (user.getRole() == null) {
                user.setRole(Role.STAFF);
            }
        } else {
            User existing = findById(user.getId());
            if (!existing.getUsername().equals(user.getUsername()) &&
                    userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new RuntimeException("Tên người dùng đã tồn tại: " + user.getUsername());
            }
            if (!existing.getEmail().equals(user.getEmail()) &&
                    userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("Email đã tồn tại: " + user.getEmail());
            }
            // Preserve existing password and createdAt
            user.setPassword(existing.getPassword());
            user.setCreatedAt(existing.getCreatedAt());
        }

        User savedUser = userRepository.save(user);

        // Send notification
        String action = isNewUser ? "đã được thêm" : "đã được cập nhật";
        String message = String.format("Nhân viên %s (%s) %s.", savedUser.getFullName(), savedUser.getRole().getLabel(), action);
        notificationService.sendNotification(
                "admin", // Default recipient for admin actions
                "Quản lý người dùng",
                message,
                "/admin/users/view/" + savedUser.getId()
        );

        return savedUser;
    }

    public void deleteUser(String id) {
        User user = findById(id);

        // Send notification
        String message = String.format("Nhân viên %s (%s) đã bị xóa.", user.getFullName(), user.getRole().getLabel());
        notificationService.sendNotification(
                "admin", // Default recipient for admin actions
                "Quản lý người dùng",
                message,
                "/admin/users"
        );

        userRepository.delete(user);
    }

    public User updateProfile(String username, User user) {
        User existingUser = findByUsername(username);

        // Validate updated fields
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new RuntimeException("Dữ liệu không hợp lệ: " + violations.iterator().next().getMessage());
        }

        if (!existingUser.getEmail().equals(user.getEmail()) &&
                userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại: " + user.getEmail());
        }

        // Update fields
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());

        return userRepository.save(existingUser);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng.");
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự, chứa ít nhất một chữ cái và một số");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void createPasswordResetToken(User user, String token) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userEmail(user.getEmail())
                .expiryDate(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(30))
                .build();
        passwordResetTokenRepository.save(resetToken);
    }

    public boolean isValidToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        return resetToken != null && resetToken.getExpiryDate().isAfter(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public boolean resetPassword(String token, String newPassword) {
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự, chứa ít nhất một chữ cái và một số");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
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
            mailMessage.setText("Chào " + user.getFullName() + ",\n\n"
                    + "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng nhấp vào liên kết để đặt lại:\n"
                    + resetUrl + "\n\n"
                    + "Liên kết này có hiệu lực trong 30 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n"
                    + "Trân trọng,\nHệ thống quản lý bãi đỗ xe");
            javaMailSender.send(mailMessage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}