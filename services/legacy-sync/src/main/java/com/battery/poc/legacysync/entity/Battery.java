package com.battery.poc.legacysync.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

// docs/generated/db-schema.md: batteries 테이블
@Getter
@Entity
@Table(name = "batteries")
public class Battery {

    @Id
    @Column(name = "battery_id")
    private String batteryId;

    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(name = "status")
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
