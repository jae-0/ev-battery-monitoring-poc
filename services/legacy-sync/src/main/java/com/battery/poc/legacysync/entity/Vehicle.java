package com.battery.poc.legacysync.entity;

import jakarta.persistence.*;
import lombok.Getter;

// docs/generated/db-schema.md: vehicles 테이블
@Getter
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "company_id")
    private String companyId;
}
