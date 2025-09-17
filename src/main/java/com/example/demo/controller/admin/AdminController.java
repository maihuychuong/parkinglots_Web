package com.example.demo.controller.admin;

import com.example.demo.config.CustomUserDetails;
import com.example.demo.entity.*;
import com.example.demo.model.dto.ParkingLogDTO;
import com.example.demo.model.enums.LogStatus;
import com.example.demo.model.enums.UserStatus;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {
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

    public AdminController(ShiftService shiftService, UserService userService, ParkingSlotRepository parkingSlotRepository,
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
    public String adminPage(Authentication auth, Model model) {
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

        long totalStaff = userRepository.count();
        model.addAttribute("totalStaff", totalStaff);

        return "admin";
    }

    // User Management Endpoints
    @GetMapping("/users")
    public String getUsers(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("role", userDetails.getUser().getRole().name());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("totalStaff", userRepository.count());
        return "admin_users";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("role", userDetails.getUser().getRole().name());
        model.addAttribute("userForm", new User());
        return "admin_user_add";
    }

    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("userForm") User user, BindingResult result, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/users/add";
        }

        try {
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm nhân viên thành công.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm nhân viên: " + e.getMessage());
            return "redirect:/admin/users/add";
        }
    }

    @GetMapping("/users/view/{id}")
    public String viewUser(@PathVariable("id") String id, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        try {
            User targetUser = userService.findById(id);
            model.addAttribute("targetUser", targetUser);
            model.addAttribute("username", username);
            model.addAttribute("role", userDetails.getUser().getRole().name());
            return "admin_user_view";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Không tìm thấy nhân viên.");
            model.addAttribute("username", username);
            return "admin_user_view";
        }
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") String id, Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        try {
            User targetUser = userService.findById(id);
            model.addAttribute("userForm", targetUser);
            model.addAttribute("username", username);
            model.addAttribute("role", userDetails.getUser().getRole().name());
            return "admin_user_edit";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Không tìm thấy nhân viên.");
            model.addAttribute("username", username);
            return "admin_user_edit";
        }
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable("id") String id, @Valid @ModelAttribute("userForm") User user, BindingResult result, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/users/edit/" + id;
        }

        try {
            user.setId(id);
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhân viên thành công.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật nhân viên: " + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") String id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa nhân viên thành công.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa nhân viên: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/notifications/{id}")
    public String viewNotification(@PathVariable("id") String id, Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        Optional<Notification> optionalNotification = notificationService.getNotificationById(id);
        if (optionalNotification.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông báo.");
            return "redirect:/admin/notifications";
        }

        Notification notification = optionalNotification.get();
        if (!notification.getRecipient().equals(username)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem thông báo này.");
            return "redirect:/admin/notifications";
        }

        notificationService.markAsRead(id, username);
        model.addAttribute("notification", notification);
        model.addAttribute("username", username);
        return "staff_notification_view";
    }

    @GetMapping("/notifications")
    public String viewAllNotifications(Authentication auth, Model model) {
        if (!isAuthenticated(auth, model, null)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        model.addAttribute("username", username);
        return "staff_notifications";
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
                    return "redirect:/admin/schedule?error=UserNotFound";
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
                    return "redirect:/admin/schedule";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    return "redirect:/admin/schedule?error=UserNotFound";
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
            return "redirect:/admin/parking-logs?error=notfound";
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
                        return "redirect:/admin/alerts?error=InvalidAlert";
                    }
                    redirectAttributes.addFlashAttribute("successMessage", "Cảnh báo đã được giải quyết thành công.");
                    return "redirect:/admin/alerts";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
                    return "redirect:/admin/alerts?error=UserNotFound";
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
        if (result.hasErrors()) {
            String errorMessage = "Vui lòng kiểm tra lại thông tin: " +
                    result.getAllErrors().stream()
                            .map(error -> error.getDefaultMessage())
                            .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/profile";
        }

        try {
            userService.updateProfile(username, user);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công.");
            return "redirect:/admin/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật thông tin: " + e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/profile";
        }
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
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(auth, null, redirectAttributes)) {
            return "redirect:/login";
        }

        String username = auth.getName();
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            return "redirect:/admin/change-password";
        }

        try {
            userService.changePassword(username, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công.");
            return "redirect:/admin/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thay đổi mật khẩu: " + e.getMessage());
            return "redirect:/admin/change-password";
        }
    }
}