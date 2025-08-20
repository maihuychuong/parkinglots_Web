package com.example.demo.service;

import com.example.demo.entity.Shift;
import com.example.demo.entity.User;
import com.example.demo.repository.ShiftRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftService {
    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    public List<Shift> getShiftsByUser(User user) {
        return shiftRepository.findByUserOrderByShiftDateDesc(user);
    }

    public List<Shift> getShiftsByUserAndDate(User user, LocalDate date) {
        return shiftRepository.findByUserAndShiftDate(user, date);
    }


    public Shift updateShiftNote(String shiftId, String userId, String note) {
        Optional<Shift> optionalShift = shiftRepository.findById(shiftId);
        if (optionalShift.isPresent() && optionalShift.get().getUser().getId().equals(userId)) {
            Shift shift = optionalShift.get();
            shift.setNote(note);
            return shiftRepository.save(shift);
        }
        throw new RuntimeException("Ca làm việc không tồn tại hoặc bạn không có quyền chỉnh sửa.");
    }

    public List<User> findStaffsByTimeRange(Instant start, Instant end) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate startDate = LocalDateTime.ofInstant(start, zone).toLocalDate();
        LocalDate endDate = LocalDateTime.ofInstant(end, zone).toLocalDate();

        List<User> staffList = new java.util.ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Shift> shifts = shiftRepository.findByShiftDate(date);

            for (Shift shift : shifts) {
                LocalTime shiftStart = shift.getShiftStart();
                LocalTime shiftEnd = shift.getShiftEnd();

                LocalTime fromTime = (date.equals(startDate)) ? LocalDateTime.ofInstant(start, zone).toLocalTime() : LocalTime.MIN;
                LocalTime toTime = (date.equals(endDate)) ? LocalDateTime.ofInstant(end, zone).toLocalTime() : LocalTime.MAX;

                boolean isOverlap = !(shiftEnd.isBefore(fromTime) || shiftStart.isAfter(toTime));
                if (isOverlap) {
                    staffList.add(shift.getUser());
                }
            }
        }

        return staffList;
    }

}