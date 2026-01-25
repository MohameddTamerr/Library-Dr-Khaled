package com.library.pos.repository;

import com.library.pos.model.WorkSession;
import com.library.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {
    List<WorkSession> findByWorkerAndStartTimeBetween(User worker, LocalDateTime start, LocalDateTime end);

    List<WorkSession> findByWorker(User worker);
}
