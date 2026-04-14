package com.battery.poc.legacysync.repository;

import com.battery.poc.legacysync.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BatteryRepository extends JpaRepository<Battery, String> {

    // 마지막 동기화 이후 변경된 배터리만 조회 (TD-03: 벌크 인서트 대상)
    List<Battery> findByUpdatedAtAfter(LocalDateTime since);
}
