package com.example.demo.data;

import com.example.demo.entity.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lớp DataSeeder để khởi tạo dữ liệu mẫu cho ứng dụng.
 * Dữ liệu được thêm vào các collection: users, parking_lots, parking_slots, pricing_rules, shifts, vehicles, parking_logs, transactions, alerts.
 * Không khai báo id để MongoDB tự tạo _id.
 * Chỉ có một quản trị viên (admin) và admin không có ca làm việc.
 * Dữ liệu trong 5 ngày gần đây (25/07/2025 - 29/07/2025).
 * Trạng thái khe đỗ (isAvailable) khớp với nhật ký đỗ xe (IN_PROGRESS -> isAvailable = false).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final ShiftRepository shiftRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    @PostConstruct
    public void seed() {
        seedUsers();
        seedParkingLot();
        seedPricingRules();
        seedShifts();
        seedVehicles();
        seedParkingLogs();
        seedTransactions();
        seedAlerts();
    }

    /**
     * Thêm dữ liệu mẫu cho người dùng (users).
     * Chỉ có một quản trị viên (admin).
     */
    private void seedUsers() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .email("admin@example.com")
                    .phone("0343875561")
                    .fullName("Quản trị viên")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(4))
                    .build();

            User staff = User.builder()
                    .username("staff")
                    .password(passwordEncoder.encode("staff"))
                    .email("staff@example.com")
                    .phone("0987654321")
                    .fullName("Nhân viên")
                    .role(Role.STAFF)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();

            User mhc = User.builder()
                    .username("chuong")
                    .password(passwordEncoder.encode("mhc123"))
                    .email("maihuychuong78@gmail.com")
                    .phone("0343875561")
                    .fullName("Mai Huy Chương")
                    .role(Role.STAFF)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();

            User staff2 = User.builder()
                    .username("staff2")
                    .password(passwordEncoder.encode("staff2"))
                    .email("staff2@example.com")
                    .phone("0945678901")
                    .fullName("Nhân viên 2")
                    .role(Role.STAFF)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            User staff3 = User.builder()
                    .username("staff3")
                    .password(passwordEncoder.encode("staff3"))
                    .email("staff3@example.com")
                    .phone("0912345678")
                    .fullName("Nhân viên 3")
                    .role(Role.STAFF)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            User staff4 = User.builder()
                    .username("staff4")
                    .password(passwordEncoder.encode("staff4"))
                    .email("staff4@example.com")
                    .phone("0923456789")
                    .fullName("Nhân viên 4")
                    .role(Role.STAFF)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            userRepository.saveAll(List.of(admin, staff, mhc, staff2, staff3, staff4));
            System.out.println("✔ Đã khởi tạo người dùng: 1 quản trị viên, 5 nhân viên.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho bãi đỗ xe (parking_lots) và khe đỗ (parking_slots).
     * Đảm bảo trạng thái isAvailable khớp với parking_logs.
     */
    private void seedParkingLot() {
        if (parkingLotRepository.count() == 0) {
            ParkingLot lot1 = ParkingLot.builder()
                    .name("Bãi xe TTTM")
                    .address("Tòa nhà Discovery Complex, 302 Cầu Giấy, Hà Nội")
                    .phone("0962287086")
                    .openTime(LocalTime.of(6, 0))
                    .closeTime(LocalTime.of(23, 0))
                    .build();

            ParkingLot lot2 = ParkingLot.builder()
                    .name("Bãi xe Royal City")
                    .address("72 Nguyễn Trãi, Thanh Xuân, Hà Nội")
                    .phone("0911122334")
                    .openTime(LocalTime.of(6, 30))
                    .closeTime(LocalTime.of(23, 30))
                    .build();

            parkingLotRepository.saveAll(List.of(lot1, lot2));

            if (parkingSlotRepository.count() == 0) {
                // Khe đỗ cho lot1 (A01-A13)
                String[] slotCodesA = {"A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10", "A11", "A12", "A13"};
                boolean[] availabilitiesA = {true, false, true, false, true, true, true, true, true, true, true, true, true}; // A02, A04 đang IN_PROGRESS
                for (int i = 0; i < slotCodesA.length; i++) {
                    ParkingSlot slot = ParkingSlot.builder()
                            .slotCode(slotCodesA[i])
                            .isAvailable(availabilitiesA[i])
                            .lot(lot1)
                            .note(availabilitiesA[i] ? "Khe đỗ tiêu chuẩn" : "Khe đỗ đang chiếm dụng")
                            .build();
                    parkingSlotRepository.save(slot);
                }

                // Khe đỗ cho lot2 (C01-C08)
                String[] slotCodesC = {"C01", "C02", "C03", "C04", "C05", "C06", "C07", "C08"};
                boolean[] availabilitiesC = {true, true, false, false, true, true, true, true}; // C03, C04 đang IN_PROGRESS
                for (int i = 0; i < slotCodesC.length; i++) {
                    ParkingSlot slot = ParkingSlot.builder()
                            .slotCode(slotCodesC[i])
                            .isAvailable(availabilitiesC[i])
                            .lot(lot2)
                            .note(availabilitiesC[i] ? "Khe đỗ tiêu chuẩn" : "Khe đỗ đang chiếm dụng")
                            .build();
                    parkingSlotRepository.save(slot);
                }
                System.out.println("✔ Đã khởi tạo khe đỗ A01-A13 và C01-C08.");
            }
            System.out.println("✔ Đã khởi tạo bãi đỗ xe.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho quy tắc giá (pricing_rules).
     * Giữ nguyên 2 quy tắc giá ban đầu.
     */
    private void seedPricingRules() {
        if (pricingRuleRepository.count() == 0) {
            PricingRule rule1 = PricingRule.builder()
                    .vehicleType(VehicleType.CAR_UNDER_9)
                    .firstBlockPrice(new BigDecimal("20000"))
                    .nextBlockPrice(new BigDecimal("15000"))
                    .blockDurationMinutes(60)
                    .createdAt(LocalDateTime.now().minusDays(4))
                    .build();

            PricingRule rule2 = PricingRule.builder()
                    .vehicleType(VehicleType.CAR_9_TO_16)
                    .firstBlockPrice(new BigDecimal("20000"))
                    .nextBlockPrice(new BigDecimal("20000"))
                    .blockDurationMinutes(60)
                    .createdAt(LocalDateTime.now().minusDays(4))
                    .build();

            pricingRuleRepository.saveAll(List.of(rule1, rule2));
            System.out.println("✔ Đã khởi tạo quy tắc giá.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho ca làm việc (shifts).
     * Quản trị viên không có ca làm việc.
     */
    private void seedShifts() {
        if (shiftRepository.count() == 0) {
            User staff = userRepository.findByUsername("staff").orElse(null);
            User mhc = userRepository.findByUsername("chuong").orElse(null);
            User staff2 = userRepository.findByUsername("staff2").orElse(null);
            User staff3 = userRepository.findByUsername("staff3").orElse(null);
            User staff4 = userRepository.findByUsername("staff4").orElse(null);

            if (staff != null && mhc != null && staff2 != null && staff3 != null && staff4 != null) {
                Shift s1 = Shift.builder()
                        .user(staff)
                        .shiftDate(LocalDate.of(2025, 7, 25))
                        .shiftType(ShiftType.MORNING)
                        .shiftStart(LocalTime.of(6, 0))
                        .shiftEnd(LocalTime.of(14, 0))
                        .note("Ca sáng cho nhân viên")
                        .build();

                Shift s2 = Shift.builder()
                        .user(mhc)
                        .shiftDate(LocalDate.of(2025, 7, 25))
                        .shiftType(ShiftType.AFTERNOON)
                        .shiftStart(LocalTime.of(14, 0))
                        .shiftEnd(LocalTime.of(19, 0))
                        .note("Ca chiều cho MHC")
                        .build();

                Shift s3 = Shift.builder()
                        .user(mhc)
                        .shiftDate(LocalDate.of(2025, 7, 25))
                        .shiftType(ShiftType.NIGHT)
                        .shiftStart(LocalTime.of(19, 0))
                        .shiftEnd(LocalTime.of(23, 0))
                        .note("Ca tối cho MHC")
                        .build();

                Shift s4 = Shift.builder()
                        .user(staff)
                        .shiftDate(LocalDate.of(2025, 7, 26))
                        .shiftType(ShiftType.MORNING)
                        .shiftStart(LocalTime.of(6, 0))
                        .shiftEnd(LocalTime.of(14, 0))
                        .note("Ca sáng cho nhân viên")
                        .build();

                Shift s5 = Shift.builder()
                        .user(mhc)
                        .shiftDate(LocalDate.of(2025, 7, 26))
                        .shiftType(ShiftType.AFTERNOON)
                        .shiftStart(LocalTime.of(14, 0))
                        .shiftEnd(LocalTime.of(19, 0))
                        .note("Ca chiều cho MHC")
                        .build();

                Shift s6 = Shift.builder()
                        .user(staff2)
                        .shiftDate(LocalDate.of(2025, 7, 27))
                        .shiftType(ShiftType.AFTERNOON)
                        .shiftStart(LocalTime.of(14, 0))
                        .shiftEnd(LocalTime.of(19, 0))
                        .note("Ca chiều cho nhân viên 2")
                        .build();

                Shift s7 = Shift.builder()
                        .user(staff2)
                        .shiftDate(LocalDate.of(2025, 7, 28))
                        .shiftType(ShiftType.AFTERNOON)
                        .shiftStart(LocalTime.of(14, 0))
                        .shiftEnd(LocalTime.of(19, 0))
                        .note("Ca chiều cho nhân viên 2")
                        .build();

                Shift s8 = Shift.builder()
                        .user(staff3)
                        .shiftDate(LocalDate.of(2025, 7, 27))
                        .shiftType(ShiftType.MORNING)
                        .shiftStart(LocalTime.of(6, 0))
                        .shiftEnd(LocalTime.of(14, 0))
                        .note("Ca sáng cho nhân viên 3")
                        .build();

                Shift s9 = Shift.builder()
                        .user(staff3)
                        .shiftDate(LocalDate.of(2025, 7, 28))
                        .shiftType(ShiftType.MORNING)
                        .shiftStart(LocalTime.of(6, 0))
                        .shiftEnd(LocalTime.of(14, 0))
                        .note("Ca sáng cho nhân viên 3")
                        .build();

                Shift s10 = Shift.builder()
                        .user(staff4)
                        .shiftDate(LocalDate.of(2025, 7, 28))
                        .shiftType(ShiftType.AFTERNOON)
                        .shiftStart(LocalTime.of(14, 0))
                        .shiftEnd(LocalTime.of(19, 0))
                        .note("Ca chiều cho nhân viên 4")
                        .build();

                Shift s11 = Shift.builder()
                        .user(staff4)
                        .shiftDate(LocalDate.of(2025, 7, 29))
                        .shiftType(ShiftType.MORNING)
                        .shiftStart(LocalTime.of(6, 0))
                        .shiftEnd(LocalTime.of(14, 0))
                        .note("Ca sáng cho nhân viên 4")
                        .build();

                shiftRepository.saveAll(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11));
                System.out.println("✔ Đã khởi tạo ca làm việc.");
            }
        }
    }

    /**
     * Thêm dữ liệu mẫu cho phương tiện (vehicles).
     */
    private void seedVehicles() {
        if (vehicleRepository.count() == 0) {
            Vehicle v1 = Vehicle.builder()
                    .plateNumber("30A-12345")
                    .vehicleType(VehicleType.CAR_UNDER_9)
                    .createdAt(LocalDateTime.now().minusDays(4))
                    .build();

            Vehicle v2 = Vehicle.builder()
                    .plateNumber("29B-67890")
                    .vehicleType(VehicleType.CAR_9_TO_16)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();

            Vehicle v3 = Vehicle.builder()
                    .plateNumber("51G-11111")
                    .vehicleType(VehicleType.CAR_UNDER_9)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            Vehicle v4 = Vehicle.builder()
                    .plateNumber("30L-22222")
                    .vehicleType(VehicleType.CAR_9_TO_16)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            Vehicle v5 = Vehicle.builder()
                    .plateNumber("29H-54321")
                    .vehicleType(VehicleType.CAR_UNDER_9)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            Vehicle v6 = Vehicle.builder()
                    .plateNumber("51F-33333")
                    .vehicleType(VehicleType.CAR_9_TO_16)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            vehicleRepository.saveAll(List.of(v1, v2, v3, v4, v5, v6));
            System.out.println("✔ Đã khởi tạo phương tiện.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho nhật ký đỗ xe (parking_logs).
     */
    private void seedParkingLogs() {
        if (parkingLogRepository.count() == 0) {
            List<Vehicle> vehicles = vehicleRepository.findAll();
            List<ParkingSlot> slots = parkingSlotRepository.findAll();
            User staff = userRepository.findByUsername("staff").orElseThrow();
            User staff2 = userRepository.findByUsername("staff2").orElse(null);
            User staff3 = userRepository.findByUsername("staff3").orElse(null);
            User staff4 = userRepository.findByUsername("staff4").orElse(null);

            Instant now = Instant.now();

            ParkingLog log1 = ParkingLog.builder()
                    .vehicle(vehicles.get(0)) // 30A-12345
                    .slot(slots.get(0)) // A01
                    .staff(staff)
                    .timeIn(now.minus(4, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS))
                    .timeOut(now.minus(4, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS))
                    .fee(new BigDecimal("40000"))
                    .status(LogStatus.COMPLETED)
                    .note("Xe ra đúng giờ")
                    .build();

            ParkingLog log2 = ParkingLog.builder()
                    .vehicle(vehicles.get(1)) // 29B-67890
                    .slot(slots.get(1)) // A02
                    .staff(staff)
                    .timeIn(now.minus(3, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS))
                    .timeOut(null)
                    .fee(null)
                    .status(LogStatus.IN_PROGRESS)
                    .note("Xe đang đỗ tại A02")
                    .build();

            ParkingLog log3 = ParkingLog.builder()
                    .vehicle(vehicles.get(2)) // 51G-11111
                    .slot(slots.get(15)) // C03
                    .staff(staff2)
                    .timeIn(now.minus(2, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS))
                    .timeOut(null)
                    .fee(null)
                    .status(LogStatus.IN_PROGRESS)
                    .note("Xe đang đỗ tại C03")
                    .build();

            ParkingLog log4 = ParkingLog.builder()
                    .vehicle(vehicles.get(3)) // 30L-22222
                    .slot(slots.get(16)) // C04
                    .staff(staff2)
                    .timeIn(now.minus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS))
                    .timeOut(null)
                    .fee(null)
                    .status(LogStatus.IN_PROGRESS)
                    .note("Xe đang đỗ tại C04")
                    .build();

            ParkingLog log5 = ParkingLog.builder()
                    .vehicle(vehicles.get(4)) // 29H-54321
                    .slot(slots.get(2)) // A03
                    .staff(staff3)
                    .timeIn(now.minus(3, ChronoUnit.DAYS).minus(4, ChronoUnit.HOURS))
                    .timeOut(now.minus(3, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS))
                    .fee(new BigDecimal("35000"))
                    .status(LogStatus.COMPLETED)
                    .note("Xe ra đúng giờ")
                    .build();

            ParkingLog log6 = ParkingLog.builder()
                    .vehicle(vehicles.get(5)) // 51F-33333
                    .slot(slots.get(3)) // A04
                    .staff(staff4)
                    .timeIn(now.minus(1, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS))
                    .timeOut(null)
                    .fee(null)
                    .status(LogStatus.IN_PROGRESS)
                    .note("Xe đang đỗ tại A04")
                    .build();

            parkingLogRepository.saveAll(List.of(log1, log2, log3, log4, log5, log6));
            System.out.println("✔ Đã khởi tạo nhật ký đỗ xe.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho giao dịch (transactions).
     */
    private void seedTransactions() {
        if (transactionRepository.count() == 0) {
            List<ParkingLog> logs = parkingLogRepository.findAll();

            List<ParkingLog> completedLogs = logs.stream()
                    .filter(log -> log.getStatus() == LogStatus.COMPLETED && log.getFee() != null)
                    .toList();

            if (completedLogs.isEmpty()) {
                System.out.println("⚠ Không có nhật ký đỗ xe hoàn thành để tạo giao dịch.");
                return;
            }

            Transaction t1 = Transaction.builder()
                    .log(completedLogs.get(0)) // log1
                    .amount(completedLogs.get(0).getFee())
                    .paidAt(LocalDateTime.now().minusDays(4).minusMinutes(5))
                    .method(TransactionMethod.CASH)
                    .status(TransactionStatus.PAID)
                    .note("Thanh toán sau khi xe rời bãi")
                    .build();

            Transaction t2 = Transaction.builder()
                    .log(completedLogs.get(0)) // log1
                    .amount(completedLogs.get(0).getFee())
                    .paidAt(LocalDateTime.now().minusDays(4))
                    .method(TransactionMethod.BANK_TRANSFER)
                    .status(TransactionStatus.PAID)
                    .note("Thanh toán bổ sung bằng chuyển khoản")
                    .build();

            Transaction t3 = Transaction.builder()
                    .log(completedLogs.get(1)) // log5
                    .amount(completedLogs.get(1).getFee())
                    .paidAt(LocalDateTime.now().minusDays(3).minusMinutes(5))
                    .method(TransactionMethod.CASH)
                    .status(TransactionStatus.PAID)
                    .note("Thanh toán sau khi xe rời bãi")
                    .build();

            transactionRepository.saveAll(List.of(t1, t2, t3));
            System.out.println("✔ Đã khởi tạo giao dịch cho các nhật ký đỗ xe hoàn thành.");
        }
    }

    /**
     * Thêm dữ liệu mẫu cho cảnh báo (alerts).
     * Sử dụng nhân viên (staff) để xử lý cảnh báo.
     */
    private void seedAlerts() {
        if (alertRepository.count() == 0) {
            List<ParkingLog> logs = parkingLogRepository.findAll();
            List<User> users = userRepository.findAll();

            if (logs.isEmpty()) {
                System.out.println("⚠ Không có nhật ký đỗ xe để tạo cảnh báo.");
                return;
            }

            if (users.isEmpty()) {
                System.out.println("⚠ Không có người dùng để gán người xử lý.");
                return;
            }

            User resolver = users.stream().filter(user -> user.getUsername().equals("staff")).findFirst().orElse(null);

            Alert alert1 = Alert.builder()
                    .type(AlertType.MISSING_EXIT)
                    .message(AlertType.MISSING_EXIT.getLabel())
                    .log(logs.get(0)) // log1
                    .createdAt(LocalDateTime.now().minusDays(4).minusHours(1))
                    .resolved(false)
                    .build();

            Alert alert2 = Alert.builder()
                    .type(AlertType.OVERDUE)
                    .message(AlertType.OVERDUE.getLabel())
                    .log(logs.get(0)) // log1
                    .createdAt(LocalDateTime.now().minusDays(4).minusMinutes(30))
                    .resolved(true)
                    .resolvedAt(LocalDateTime.now().minusDays(4).minusMinutes(10))
                    .resolvedBy(resolver)
                    .build();

            Alert alert3 = Alert.builder()
                    .type(AlertType.OVERDUE)
                    .message(AlertType.OVERDUE.getLabel())
                    .log(logs.get(2)) // log3
                    .createdAt(LocalDateTime.now().minusDays(2).minusMinutes(15))
                    .resolved(false)
                    .build();

            Alert alert4 = Alert.builder()
                    .type(AlertType.OVERDUE)
                    .message(AlertType.OVERDUE.getLabel())
                    .log(logs.get(3)) // log4
                    .createdAt(LocalDateTime.now().minusDays(1).minusMinutes(15))
                    .resolved(false)
                    .build();

            Alert alert5 = Alert.builder()
                    .type(AlertType.OVERDUE)
                    .message(AlertType.OVERDUE.getLabel())
                    .log(logs.get(4)) // log5
                    .createdAt(LocalDateTime.now().minusDays(3).minusMinutes(30))
                    .resolved(true)
                    .resolvedAt(LocalDateTime.now().minusDays(3).minusMinutes(10))
                    .resolvedBy(resolver)
                    .build();

            Alert alert6 = Alert.builder()
                    .type(AlertType.OVERDUE)
                    .message(AlertType.OVERDUE.getLabel())
                    .log(logs.get(5)) // log6
                    .createdAt(LocalDateTime.now().minusDays(1).minusMinutes(15))
                    .resolved(false)
                    .build();

            alertRepository.saveAll(List.of(alert1, alert2, alert3, alert4, alert5, alert6));
            System.out.println("✔ Đã khởi tạo cảnh báo.");
        }
    }
}