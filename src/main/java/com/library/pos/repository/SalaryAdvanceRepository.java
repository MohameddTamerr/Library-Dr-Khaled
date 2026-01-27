package com.library.pos.repository;

import com.library.pos.model.SalaryAdvance;
import com.library.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {
    List<SalaryAdvance> findByWorkerAndAdvanceDateBetween(User worker, LocalDate start, LocalDate end);

    List<SalaryAdvance> findByWorker(User worker);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(a.id) FROM SalaryAdvance a")
    Long findLatestId();
}
