package com.library.pos.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "worker_shifts")
public class WorkerShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column
    private String notes;

    public WorkerShift() {
    }

    // Getters
    public Long getId() {
        return id;
    }

    public User getWorker() {
        return worker;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setWorker(User worker) {
        this.worker = worker;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
