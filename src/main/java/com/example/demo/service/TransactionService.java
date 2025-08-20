package com.example.demo.service;

import com.example.demo.entity.Alert;
import com.example.demo.entity.Transaction;
import com.example.demo.model.enums.AlertType;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private static final Duration PENDING_TIMEOUT = Duration.ofMinutes(10);
    public List<Transaction> getAllTransactions(String status) {
        if (status == null || status.isEmpty()) {
            return transactionRepository.findAll();
        }

        TransactionStatus transactionStatus;
        try {
            transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }

        return transactionRepository.findByStatus(transactionStatus);
    }

    public long getTodayTransactionCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return transactionRepository.countByPaidAtBetween(startOfDay, endOfDay);
    }

    // Hàm này gọi định kỳ hoặc theo nhu cầu để xử lý transaction PENDING quá lâu và tạo alert khi FAIL
    public void processPendingTransactions() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy danh sách giao dịch PENDING trước thời điểm timeout
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        for (Transaction tx : pendingTransactions) {
            LocalDateTime paidAt = tx.getPaidAt();
            // Nếu paidAt == null hoặc thời gian quá lâu thì cập nhật trạng thái
            if (paidAt == null || Duration.between(paidAt, now).compareTo(PENDING_TIMEOUT) > 0) {
                // Cập nhật trạng thái sang FAILED
                tx.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(tx);

                // Tạo alert báo lỗi thanh toán
                Alert alert = Alert.builder()
                        .type(AlertType.PAYMENT_ERROR)
                        .message("Thanh toán quá hạn, chuyển trạng thái sang FAILED cho lượt đỗ xe ID: " + tx.getLog().getId())
                        .log(tx.getLog())
                        .createdAt(now)
                        .resolved(false)
                        .build();
                alertService.createAlertAndNotify(alert);
            }
        }
    }

    // Hàm save transaction với alert khi trạng thái FAILED
    public Transaction saveTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);

        if (saved.getStatus() == TransactionStatus.FAILED) {
            // Kiểm tra xem đã có alert chưa, tránh trùng
            boolean exists = alertRepository.existsByLogAndTypeAndResolvedFalse(saved.getLog(), AlertType.PAYMENT_ERROR);
            if (!exists) {
                Alert alert = Alert.builder()
                        .type(AlertType.PAYMENT_ERROR)
                        .message("Thanh toán thất bại cho lượt đỗ xe ID: " + saved.getLog().getId())
                        .log(saved.getLog())
                        .createdAt(LocalDateTime.now())
                        .resolved(false)
                        .build();
                alertService.createAlertAndNotify(alert);
            }
        }

        return saved;
    }

    @Scheduled(fixedRate = 300000) // 5 phút một lần
    public void scheduledPendingCheck() {
        processPendingTransactions();
    }
}
