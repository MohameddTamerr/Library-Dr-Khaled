package com.library.pos.service;

import com.library.pos.model.Role;
import com.library.pos.model.User;
import com.library.pos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final com.library.pos.repository.SalaryAdvanceRepository advanceRepository;
    private final com.library.pos.repository.WorkSessionRepository sessionRepository;

    @Autowired
    public UserService(UserRepository userRepository,
            com.library.pos.repository.SalaryAdvanceRepository advanceRepository,
            com.library.pos.repository.WorkSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.advanceRepository = advanceRepository;
        this.sessionRepository = sessionRepository;
    }

    public List<User> getAllWorkers() {
        List<User> workers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.WORKER)
                .collect(Collectors.toList());

        // Populate current withdrawals
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate start = now.withDayOfMonth(1);
        java.time.LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        workers.forEach(w -> {
            Double drawn = advanceRepository.findByWorkerAndAdvanceDateBetween(w, start, end).stream()
                    .mapToDouble(com.library.pos.model.SalaryAdvance::getAmount)
                    .sum();
            w.setCurrentWithdrawal(drawn);
        });

        return workers;
    }

    public List<User> getDeliveryMen() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.DELIVERY_MEN);
    }

    public List<User> getStaff() {
        return userRepository.findByRoleInOrderByFullNameAsc(List.of(Role.WORKER, Role.DELIVERY_MEN));
    }

    public long getWorkMinutes(User worker, java.time.LocalDate start, java.time.LocalDate end) {
        return sessionRepository
                .findByWorkerAndStartTimeBetween(worker, start.atStartOfDay(), end.atTime(java.time.LocalTime.MAX))
                .stream()
                .mapToLong(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
    }

    public void addAdvance(User worker, Double amount, String note) {
        com.library.pos.model.SalaryAdvance adv = new com.library.pos.model.SalaryAdvance();
        adv.setWorker(worker);
        adv.setAmount(amount);
        adv.setAdvanceDate(java.time.LocalDate.now());
        adv.setReason(note);
        adv.setIsDeducted(false);
        advanceRepository.save(adv);
    }

    public User saveWorker(User user) {
        user.setRole(Role.WORKER);
        return userRepository.save(user);
    }

    public User saveStaff(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public java.time.LocalDateTime getWorkersLatestUpdateTime() {
        return userRepository.findLatestUpdateByRole(Role.WORKER);
    }

    public long getWorkersCount() {
        return userRepository.countByRole(Role.WORKER);
    }

    public java.time.LocalDateTime getStaffLatestUpdateTime() {
        return userRepository.findLatestUpdateByRoles(List.of(Role.WORKER, Role.DELIVERY_MEN));
    }

    public long getStaffCount() {
        return userRepository.countByRoleIn(List.of(Role.WORKER, Role.DELIVERY_MEN));
    }

    public Long getLatestAdvanceId() {
        return advanceRepository.findLatestId();
    }

    public long getAdvanceCount() {
        return advanceRepository.count();
    }

    public String generateDeliveryUsername(String name, String phone) {
        String base = "delivery";
        String digits = phone != null ? phone.replaceAll("[^0-9]", "") : "";
        if (!digits.isBlank()) {
            String suffix = digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
            base = "delivery" + suffix;
        } else if (name != null && !name.isBlank()) {
            base = "delivery" + Math.abs(name.hashCode());
        }
        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + counter++;
        }
        return candidate;
    }

    public String generateDeliveryPassword() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
