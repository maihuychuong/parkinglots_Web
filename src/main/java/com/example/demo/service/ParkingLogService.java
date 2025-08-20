package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.model.enums.AlertType;
import com.example.demo.model.enums.LogStatus;
import com.example.demo.model.enums.VehicleType;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.ParkingLogRepository;
import com.example.demo.repository.PricingRuleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ParkingLogService {

    private final ParkingLogRepository parkingLogRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final ShiftService shiftService;
    private final AlertRepository alertRepository;
    private final AlertService alertService;

    public ParkingLogService(ParkingLogRepository parkingLogRepository,
                             PricingRuleRepository pricingRuleRepository, ShiftService shiftService, AlertRepository alertRepository, AlertService alertService) {
        this.parkingLogRepository = parkingLogRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.shiftService = shiftService;
        this.alertRepository = alertRepository;
        this.alertService = alertService;
    }

    // Hàm tính phí dựa trên thời gian và loại xe lấy từ pricing_rules
    private BigDecimal calculateFee(Instant timeIn, Instant timeOut, VehicleType vehicleType) {
        if (timeIn == null || timeOut == null || timeOut.isBefore(timeIn)) {
            return BigDecimal.ZERO;
        }

        long durationMinutes = Duration.between(timeIn, timeOut).toMinutes();
        if (durationMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        PricingRule rule = pricingRuleRepository.findByVehicleType(vehicleType)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng giá cho loại xe: " + vehicleType));

        int blockDuration = rule.getBlockDurationMinutes() != null ? rule.getBlockDurationMinutes() : 60;
        int blocks = (int) Math.ceil((double) durationMinutes / blockDuration);

        if (blocks <= 1) {
            return rule.getFirstBlockPrice();
        } else {
            BigDecimal nextBlocksCost = rule.getNextBlockPrice().multiply(BigDecimal.valueOf(blocks - 1));
            return rule.getFirstBlockPrice().add(nextBlocksCost);
        }
    }


    // Phương thức tạo hoặc cập nhật ParkingLog, tự động tính phí khi có timeOut
    public ParkingLog saveOrUpdateParkingLog(ParkingLog log) {
        if (log.getTimeOut() != null && log.getTimeIn() != null && log.getVehicle() != null) {
            BigDecimal fee = calculateFee(log.getTimeIn(), log.getTimeOut(), log.getVehicle().getVehicleType());
            log.setFee(fee);
        }
        return parkingLogRepository.save(log);
    }

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String formatInstantToVn(Instant instant) {
        if (instant == null) return "";
        return instant.atZone(VN_ZONE).format(FORMATTER);
    }

    public List<User> getStaffsInChargeDuringParking(ParkingLog log) {
        if (log.getTimeIn() == null || log.getTimeOut() == null) {
            return List.of();
        }

        return shiftService.findStaffsByTimeRange(log.getTimeIn(), log.getTimeOut());
    }

    @Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút
    public void checkOvertimeParking() {
        List<ParkingLog> activeLogs = parkingLogRepository.findByStatus(LogStatus.IN_PROGRESS); // hoặc checkOut == null

        LocalDateTime now = LocalDateTime.now();

        for (ParkingLog log : activeLogs) {
            ParkingLot lot = log.getSlot().getLot();
            LocalTime endTime = lot.getCloseTime();

            // Nếu thời gian hiện tại đã vượt quá giờ đóng cửa của bãi
            if (now.toLocalTime().isAfter(endTime)) {
                // Tránh tạo alert trùng lặp
                boolean exists = alertRepository.existsByLogAndTypeAndResolvedFalse(log, AlertType.OVERDUE);
                if (!exists) {
                    Alert alert = Alert.builder()
                            .type(AlertType.OVERDUE)
                            .message("Xe đang đỗ quá giờ hoạt động tại bãi: " + lot.getName() + " - Log ID: " + log.getId())
                            .log(log)
                            .createdAt(now)
                            .resolved(false)
                            .build();
                    alertService.createAlertAndNotify(alert); // gửi luôn thông báo
                }
            }
        }
    }

    @Scheduled(fixedRate = 3600000) // 1 tiếng kiểm tra 1 lần
    public void checkMissingExitLogs() {
        LocalDateTime now = LocalDateTime.now();
        List<ParkingLog> logs = parkingLogRepository.findByStatus(LogStatus.IN_PROGRESS);

        for (ParkingLog log : logs) {
            // Nếu đã quá 24 giờ kể từ khi check-in mà chưa check-out
            if (Duration.between(log.getTimeIn(), now).toHours() >= 24 && log.getTimeOut() == null) {
                boolean exists = alertRepository.existsByLogAndTypeAndResolvedFalse(log, AlertType.MISSING_EXIT);
                if (!exists) {
                    Alert alert = Alert.builder()
                            .type(AlertType.MISSING_EXIT)
                            .message("Xe đã đỗ quá 24 giờ mà chưa check-out - Log ID: " + log.getId())
                            .log(log)
                            .createdAt(now)
                            .resolved(false)
                            .build();
                    alertService.createAlertAndNotify(alert); // gửi luôn thông báo
                }
            }
        }
    }

}
