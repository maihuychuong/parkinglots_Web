package com.example.demo.service;

import com.example.demo.entity.ParkingLog;
import com.example.demo.entity.ParkingLot;
import com.example.demo.entity.ParkingSlot;
import com.example.demo.entity.Transaction;
import com.example.demo.model.enums.LogStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.repository.ParkingLogRepository;
import com.example.demo.repository.ParkingLotRepository;
import com.example.demo.repository.ParkingSlotRepository;
import com.example.demo.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final ParkingLogRepository parkingLogRepository;

    private final ParkingSlotRepository parkingSlotRepository;

    private final TransactionRepository transactionRepository;

    private final ParkingLotRepository parkingLotRepository;

    public ReportService(ParkingLogRepository parkingLogRepository, ParkingSlotRepository parkingSlotRepository, TransactionRepository transactionRepository, ParkingLotRepository parkingLotRepository) {
        this.parkingLogRepository = parkingLogRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.transactionRepository = transactionRepository;
        this.parkingLotRepository = parkingLotRepository;
    }

    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAllByOrderByNameAsc();
    }

    public long getTotalVehicles(LocalDate startDate, LocalDate endDate, String lotId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<ParkingLog> logs = parkingLogRepository.findByTimeInBetween(start, end);
        if (lotId == null || lotId.isEmpty()) {
            return logs.size();
        }
        List<ParkingSlot> slots = parkingSlotRepository.findByLot_Id(lotId);
        List<String> slotIds = slots.stream().map(ParkingSlot::getId).collect(Collectors.toList());
        return logs.stream()
                .filter(log -> log.getSlot() != null && slotIds.contains(log.getSlot().getId()))
                .count();
    }

    public double getSlotUsagePercentage(LocalDate startDate, LocalDate endDate, String lotId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        long totalSlots = lotId == null || lotId.isEmpty()
                ? parkingSlotRepository.count()
                : parkingSlotRepository.findByLot_Id(lotId).size();
        List<ParkingLog> activeLogs = parkingLogRepository.findByStatus(LogStatus.IN_PROGRESS);
        long usedSlots = lotId == null || lotId.isEmpty()
                ? activeLogs.size()
                : activeLogs.stream()
                .filter(log -> log.getSlot() != null && log.getSlot().getLot() != null
                        && lotId.equals(log.getSlot().getLot().getId()))
                .count();
        return totalSlots > 0 ? (usedSlots * 100.0 / totalSlots) : 0.0;
    }

    public double getTotalRevenue(LocalDate startDate, LocalDate endDate, String lotId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PAID)
                .stream()
                .filter(t -> t.getPaidAt() != null && !t.getPaidAt().isBefore(start) && !t.getPaidAt().isAfter(end))
                .collect(Collectors.toList());
        if (lotId == null || lotId.isEmpty()) {
            return transactions.stream()
                    .map(Transaction::getAmount)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
        }
        List<ParkingSlot> slots = parkingSlotRepository.findByLot_Id(lotId);
        List<String> slotIds = slots.stream().map(ParkingSlot::getId).collect(Collectors.toList());
        return transactions.stream()
                .filter(t -> t.getLog() != null && t.getLog().getSlot() != null
                        && slotIds.contains(t.getLog().getSlot().getId()))
                .map(Transaction::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    public Map<String, Integer> getVehicleChartData(LocalDate startDate, LocalDate endDate, String lotId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        System.out.println("Fetching ParkingLogs between " + start + " and " + end + " for lotId: " + lotId);
        List<ParkingSlot> slots = lotId == null || lotId.isEmpty() ? null : parkingSlotRepository.findByLot_Id(lotId);
        List<String> slotIds = slots != null ? slots.stream().map(ParkingSlot::getId).collect(Collectors.toList()) : null;

        List<ParkingLog> logs = parkingLogRepository.findByTimeInBetween(start, end);
        System.out.println("Found " + logs.size() + " ParkingLogs");
        Map<LocalDate, Long> counts = logs.stream()
                .filter(log -> {
                    boolean valid = log.getSlot() != null && (slotIds == null || slotIds.contains(log.getSlot().getId()));
                    if (!valid) {
                        System.out.println("Filtered out log: " + log.getId() + " due to null slot or invalid lot");
                    }
                    return valid;
                })
                .collect(Collectors.groupingBy(
                        log -> {
                            LocalDate date = log.getTimeIn().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
                            System.out.println("Log ID: " + log.getId() + ", Date: " + date);
                            return date;
                        },
                        Collectors.counting()
                ));

        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            result.put(current.toString(), counts.getOrDefault(current, 0L).intValue());
            current = current.plusDays(1);
        }
        System.out.println("Vehicle chart data: " + result);
        return result;
    }

    public Map<String, Double> getRevenueChartData(LocalDate startDate, LocalDate endDate, String lotId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        System.out.println("Fetching Transactions between " + start + " and " + end + " for lotId: " + lotId);
        List<ParkingSlot> slots = lotId == null || lotId.isEmpty() ? null : parkingSlotRepository.findByLot_Id(lotId);
        List<String> slotIds = slots != null ? slots.stream().map(ParkingSlot::getId).collect(Collectors.toList()) : null;

        List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PAID)
                .stream()
                .filter(t -> {
                    boolean valid = t.getPaidAt() != null && !t.getPaidAt().isBefore(start) && !t.getPaidAt().isAfter(end)
                            && t.getLog() != null && t.getLog().getSlot() != null
                            && (slotIds == null || slotIds.contains(t.getLog().getSlot().getId()));
                    if (!valid) {
                        System.out.println("Filtered out transaction: " + t.getId() + " due to invalid paidAt, log, or slot");
                    }
                    return valid;
                })
                .collect(Collectors.toList());

        Map<LocalDate, Double> revenue = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            LocalDate date = t.getPaidAt().toLocalDate();
                            System.out.println("Transaction ID: " + t.getId() + ", Date: " + date + ", Amount: " + t.getAmount());
                            return date;
                        },
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())
                ));

        Map<String, Double> result = new LinkedHashMap<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            result.put(current.toString(), revenue.getOrDefault(current, 0.0));
            current = current.plusDays(1);
        }
        System.out.println("Revenue chart data: " + result);
        return result;
    }
}