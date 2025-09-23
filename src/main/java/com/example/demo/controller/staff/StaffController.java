package com.example.demo.controller.staff;

import com.example.demo.config.CustomUserDetails;
import com.example.demo.entity.*;
import com.example.demo.model.dto.ParkingLogDTO;
import com.example.demo.model.enums.LogStatus;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
public class StaffController {
    private final ShiftService shiftService;
    private final UserService userService;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLogService parkingLogService;
    private final ParkingLotService parkingLotService;
    private final ParkingSlotService parkingSlotService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public StaffController(ShiftService shiftService, UserService userService, ParkingSlotRepository parkingSlotRepository,
                           ParkingLogRepository parkingLogRepository, VehicleRepository vehicleRepository,
                           ParkingLogService parkingLogService, ParkingLotService parkingLotService,
                           ParkingSlotService parkingSlotService, TransactionService transactionService,
                           TransactionRepository transactionRepository, AlertService alertService,
                           AlertRepository alertRepository, UserRepository userRepository,
                           PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.shiftService = shiftService;
        this.userService = userService;
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingLogRepository = parkingLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingLogService = parkingLogService;
        this.parkingLotService = parkingLotService;
        this.parkingSlotService = parkingSlotService;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.alertService = alertService;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    // Updated @ModelAttribute to use NotificationService
    @ModelAttribute
    public void addNotifications(Authentication auth, Model model) {
        if (isAuthenticated(auth, model, null)) {
            String username = auth.getName();
            List<Notification> notifications = notificationService.getNotificationsForUser(username);
            long unreadCount = notificationService.countUnreadNotifications(username);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
        } else {
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("unreadCount", 0);
        }
    }

    private boolean isAuthenticated(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) {
            if (model != null) {
                model.addAttribute("errorMessage", "Vui lòng đăng nhập để truy cập.");
            } else if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để truy cập.");
            }
            return false;
        }
        return true;
    }

    @GetMapping
    public String staffPage(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("role", userDetails.getUser().getRole().name());

        long availableSlots = parkingSlotRepository.countByIsAvailableTrue();
        model.addAttribute("availableSlots", availableSlots);

        long inProgressLogs = parkingLogRepository.countByStatus(LogStatus.IN_PROGRESS);
        model.addAttribute("activeParkingLogs", inProgressLogs);

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayTransactions = transactionRepository.countByPaidAtBetween(startOfDay, endOfDay);
        model.addAttribute("todayTransactions", todayTransactions);

        long unresolvedAlertsCount = alertRepository.countByResolvedFalse();
        model.addAttribute("unresolvedAlerts", unresolvedAlertsCount);

        return "staff";
    }

    // Updated /notifications/{id} to use NotificationService
    @GetMapping("/notifications/{id}")
    public String viewNotification(@PathVariable("id") String id, Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        Optional<Notification> optionalNotification = notificationService.getNotificationById(id); // Add this method to NotificationService if needed
        if (optionalNotification.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông báo.");
            return "redirect:/staff/notifications";
        }

        Notification notification = optionalNotification.get();
        if (!notification.getRecipient().equals(username)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem thông báo này.");
            return "redirect:/staff/notifications";
        }

        // Mark notification as read
        notificationService.markAsRead(id, username);

        model.addAttribute("notification", notification);
        model.addAttribute("username", username);
        return "notification_view";
    }

    // Existing /notifications endpoint (unchanged)
    @GetMapping("/notifications")
    public String viewAllNotifications(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);
        return "notifications";
    }

    @GetMapping("/schedule")
    public String getSchedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    List<Shift> shifts;
                    if (date != null) {
                        shifts = shiftService.getShiftsByUserAndDate(user, date);
                        model.addAttribute("selectedDate", date);
                    } else {
                        shifts = shiftService.getShiftsByUser(user);
                    }
                    model.addAttribute("shifts", shifts);
                    model.addAttribute("username", user.getUsername());
                    return "staff_schedule";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("shifts", Collections.emptyList());
                    model.addAttribute("username", "Unknown");
                    return "staff_schedule";
                });
    }

    @GetMapping("/schedule/edit/{id}")
    public String showEditNoteForm(@PathVariable("id") String shiftId, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    Shift shift = shiftService.getShiftsByUser(user).stream()
                            .filter(s -> s.getId().equals(shiftId))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Ca làm việc không tồn tại."));
                    model.addAttribute("shift", shift);
                    model.addAttribute("username", user.getUsername());
                    return "staff_schedule_edit";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("username", "Unknown");
                    return "redirect:/staff/schedule?error=UserNotFound";
                });
    }

    @PostMapping("/schedule/edit/{id}")
    public String updateShiftNote(@PathVariable("id") String shiftId, @RequestParam("note") String note,
                                  Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    shiftService.updateShiftNote(shiftId, user.getId(), note);
                    return "redirect:/staff/schedule";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    return "redirect:/staff/schedule?error=UserNotFound";
                });
    }

    @GetMapping("/parking-logs")
    public String viewParkingLogs(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);

        List<ParkingLog> logs;
        if (date != null) {
            if (date.isAfter(LocalDate.now())) {
                model.addAttribute("errorMessage", "Không thể tìm kiếm ngày trong tương lai.");
                logs = parkingLogRepository.findAll();
            } else {
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(23, 59, 59);
                logs = parkingLogRepository.findByTimeInBetween(start, end);
                model.addAttribute("selectedDate", date);
            }
        } else {
            logs = parkingLogRepository.findAll();
        }

        List<ParkingLogDTO> dtos = logs.stream().map(log -> {
            Vehicle vehicle = log.getVehicle();
            ParkingSlot slot = log.getSlot();
            User staff = log.getStaff();

            return ParkingLogDTO.builder()
                    .id(log.getId())
                    .plateNumber(vehicle != null ? vehicle.getPlateNumber() : "")
                    .vehicleType(vehicle != null ? vehicle.getVehicleType().getLabel() : "")
                    .slotCode(slot != null ? slot.getSlotCode() : "")
                    .staffName(staff != null ? staff.getFullName() : "")
                    .timeIn(parkingLogService.formatInstantToVn(log.getTimeIn()))
                    .timeOut(log.getTimeOut() != null
                            ? parkingLogService.formatInstantToVn(log.getTimeOut())
                            : "Chưa ra")
                    .fee(log.getFee())
                    .status(log.getStatus().getLabel())
                    .note(log.getNote())
                    .build();
        }).toList();

        model.addAttribute("parkingLogs", dtos);
        return "staff_parking_logs";
    }

    @GetMapping("/parking-logs/view/{id}")
    public String viewParkingLogDetail(@PathVariable("id") String id, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);

        Optional<ParkingLog> optionalLog = parkingLogRepository.findById(id);
        if (optionalLog.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy bản ghi.");
            return "redirect:/staff/parking-logs?error=notfound";
        }

        ParkingLog log = optionalLog.get();
        log = parkingLogService.saveOrUpdateParkingLog(log);

        Vehicle vehicle = vehicleRepository.findById(log.getVehicle().getId()).orElse(null);
        ParkingSlot slot = parkingSlotRepository.findById(log.getSlot().getId()).orElse(null);

        List<User> staffs = parkingLogService.getStaffsInChargeDuringParking(log);
        String staffNames = staffs.stream()
                .map(User::getFullName)
                .distinct()
                .collect(Collectors.joining(", "));

        String timeInStr = parkingLogService.formatInstantToVn(log.getTimeIn());
        String timeOutStr = log.getTimeOut() != null
                ? parkingLogService.formatInstantToVn(log.getTimeOut())
                : "Chưa ra";

        ParkingLogDTO dto = ParkingLogDTO.builder()
                .id(log.getId())
                .plateNumber(vehicle != null ? vehicle.getPlateNumber() : "")
                .vehicleType(vehicle != null ? vehicle.getVehicleType().getLabel() : "")
                .slotCode(slot != null ? slot.getSlotCode() : "")
                .staffName(staffNames)
                .timeIn(timeInStr)
                .timeOut(timeOutStr)
                .fee(log.getFee())
                .status(log.getStatus().getLabel())
                .note(log.getNote())
                .build();

        model.addAttribute("parkingLog", dto);
        return "staff_parking_log_view";
    }

    @GetMapping("/parking-slots")
    public String listParkingSlots(@RequestParam(required = false) String lotId, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);

        List<ParkingSlot> parkingSlots;
        if (lotId != null && !lotId.isEmpty()) {
            parkingSlots = parkingSlotService.findByLotId(lotId);
        } else {
            parkingSlots = parkingSlotService.findAll();
        }

        List<ParkingLot> parkingLots = parkingLotService.findAll();

        model.addAttribute("parkingSlots", parkingSlots);
        model.addAttribute("parkingLots", parkingLots);
        model.addAttribute("selectedLotId", lotId);

        return "staff_parking_slots";
    }

    @GetMapping("/parking-lots")
    public String getParkingLots(@RequestParam(value = "name", required = false) String name, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    model.addAttribute("parkingLots", parkingLotService.getAllParkingLots(name));
                    model.addAttribute("username", user.getUsername());
                    return "parking-lots";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("parkingLots", Collections.emptyList());
                    model.addAttribute("username", "Unknown");
                    return "parking-lots";
                });
    }

    @GetMapping("/transactions")
    public String getTransactions(@RequestParam(value = "status", required = false) String status, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    model.addAttribute("transactions", transactionService.getAllTransactions(status));
                    model.addAttribute("username", user.getUsername());
                    return "transactions";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("transactions", Collections.emptyList());
                    model.addAttribute("username", "Unknown");
                    return "transactions";
                });
    }

    @GetMapping("/alerts")
    public String getAlerts(@RequestParam(value = "type", required = false) String type,
                            @RequestParam(value = "resolved", required = false) String resolved,
                            Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    List<Alert> alerts = alertService.getAllAlerts(type, resolved);
                    model.addAttribute("alerts", alerts);
                    model.addAttribute("username", user.getUsername());
                    model.addAttribute("role", user.getRole());
                    return "alerts";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("alerts", Collections.emptyList());
                    model.addAttribute("username", "Unknown");
                    return "alerts";
                });
    }

    @PostMapping("/alerts/resolve/{id}")
    public String resolveAlert(@PathVariable("id") String id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    Alert resolvedAlert = alertService.markAlertAsResolved(id, username);
                    if (resolvedAlert == null) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Cảnh báo không tồn tại hoặc đã được giải quyết.");
                        return "redirect:/staff/alerts?error=InvalidAlert";
                    }
                    redirectAttributes.addFlashAttribute("successMessage", "Cảnh báo đã được giải quyết thành công.");
                    return "redirect:/staff/alerts";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    return "redirect:/staff/alerts?error=UserNotFound";
                });
    }

    @GetMapping("/alerts/view/{id}")
    public String viewAlert(@PathVariable("id") String id, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    try {
                        Alert alert = alertService.getAlertById(id);
                        model.addAttribute("alert", alert);
                        model.addAttribute("username", user.getUsername());
                        model.addAttribute("role", user.getRole());
                        return "alert_view";
                    } catch (RuntimeException e) {
                        model.addAttribute("errorMessage", "Không tìm thấy cảnh báo.");
                        model.addAttribute("username", user.getUsername());
                        return "alert_view";
                    }
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("username", "Unknown");
                    return "alert_view";
                });
    }

    @GetMapping("/profile")
    public String getProfile(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        return userService.findByUsernameSafe(username)
                .map(user -> {
                    model.addAttribute("user", user);
                    model.addAttribute("username", user.getUsername());
                    model.addAttribute("role", user.getRole().name());
                    return "profile";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Không tìm thấy người dùng.");
                    model.addAttribute("username", "Unknown");
                    return "profile";
                });
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("user") User user, BindingResult result,
                                Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        System.out.println("Received POST /staff/profile for username: " + username +
                ", Data: fullName=" + user.getFullName() +
                ", email=" + user.getEmail() +
                ", phone=" + user.getPhone());

        if (result.hasErrors()) {
            String errorMessage = "Vui lòng kiểm tra lại thông tin: " +
                    result.getAllErrors().stream()
                            .map(error -> error.getDefaultMessage())
                            .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            System.out.println("Validation errors: " + errorMessage);
            return "redirect:/staff/profile";
        }

        return userService.findByUsernameSafe(username)
                .map(existingUser -> {
                    System.out.println("Found existing user: " + existingUser);

                    if (!existingUser.getEmail().equals(user.getEmail()) &&
                            userRepository.findByEmail(user.getEmail()).isPresent()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại.");
                        redirectAttributes.addFlashAttribute("user", user);
                        System.out.println("Email already exists: " + user.getEmail());
                        return "redirect:/staff/profile";
                    }

                    existingUser.setFullName(user.getFullName());
                    existingUser.setEmail(user.getEmail());
                    existingUser.setPhone(user.getPhone());
                    try {
                        userRepository.save(existingUser);
                        System.out.println("User saved successfully: " + existingUser);
                        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công.");
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu thông tin: " + e.getMessage());
                        redirectAttributes.addFlashAttribute("user", user);
                        System.out.println("Error saving user: " + e.getMessage());
                        return "redirect:/staff/profile";
                    }

                    return "redirect:/staff/profile";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    System.out.println("User not found for username: " + username);
                    return "redirect:/staff/profile";
                });
    }

    @GetMapping("/change-password")
    public String getChangePassword(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);
        return "change_password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$", message = "Mật khẩu mới phải có ít nhất 6 ký tự, bao gồm chữ cái và số") String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            System.out.println("Password confirmation does not match for user: " + username);
            return "redirect:/staff/change-password";
        }

        return userService.findByUsernameSafe(username)
                .map(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không đúng.");
                        System.out.println("Incorrect current password for user: " + username);
                        return "redirect:/staff/change-password";
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    try {
                        userRepository.save(user);
                        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công.");
                        System.out.println("Password changed successfully for user: " + username);
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu mật khẩu: " + e.getMessage());
                        System.out.println("Error changing password for user: " + username + ", Error: " + e.getMessage());
                    }
                    return "redirect:/staff/change-password";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    System.out.println("User not found for username: " + username);
                    return "redirect:/staff/change-password";
                });
    }
}