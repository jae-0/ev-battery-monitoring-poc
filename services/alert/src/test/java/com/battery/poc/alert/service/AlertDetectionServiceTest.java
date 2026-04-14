package com.battery.poc.alert.service;

import com.battery.poc.alert.domain.AlertEvent;
import com.battery.poc.alert.domain.TelemetryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AlertDetectionServiceTest {

    private AlertDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AlertDetectionService();
    }

    private TelemetryEvent event(double temp, double voltage) {
        TelemetryEvent e = new TelemetryEvent();
        e.setBatteryId("BAT-TEST");
        e.setVehicleId("VEH-TEST");
        e.setTimestamp(System.currentTimeMillis());
        e.setTemperature(temp);
        e.setVoltage(voltage);
        return e;
    }

    @Test
    @DisplayName("정상 범위 — 알림 없음")
    void detect_normalRange_noAlerts() {
        List<AlertEvent> alerts = service.detect(event(35.0, 400.0));
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("온도 50°C 초과 → WARNING")
    void detect_tempAbove50_warningAlert() {
        List<AlertEvent> alerts = service.detect(event(55.0, 400.0));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertEvent.AlertType.TEMPERATURE_EXCEEDED);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertEvent.Severity.WARNING);
    }

    @Test
    @DisplayName("온도 60°C 초과 → CRITICAL")
    void detect_tempAbove60_criticalAlert() {
        List<AlertEvent> alerts = service.detect(event(65.0, 400.0));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertEvent.AlertType.TEMPERATURE_EXCEEDED);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertEvent.Severity.CRITICAL);
    }

    @Test
    @DisplayName("온도 정확히 60°C → WARNING (50 초과, 60 이하)")
    void detect_tempExactly60_warningAlert() {
        List<AlertEvent> alerts = service.detect(event(60.0, 400.0));
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertEvent.Severity.WARNING);
    }

    @Test
    @DisplayName("전압 300V 미만 → CRITICAL")
    void detect_voltageBelowMin_criticalAlert() {
        List<AlertEvent> alerts = service.detect(event(35.0, 250.0));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertEvent.AlertType.VOLTAGE_CRITICAL);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertEvent.Severity.CRITICAL);
    }

    @Test
    @DisplayName("전압 450V 초과 → CRITICAL")
    void detect_voltageAboveMax_criticalAlert() {
        List<AlertEvent> alerts = service.detect(event(35.0, 500.0));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertEvent.AlertType.VOLTAGE_CRITICAL);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertEvent.Severity.CRITICAL);
    }

    @Test
    @DisplayName("고온 + 이상 전압 동시 → 알림 2개")
    void detect_tempAndVoltageAnomaly_twoAlerts() {
        List<AlertEvent> alerts = service.detect(event(65.0, 500.0));
        assertThat(alerts).hasSize(2);
    }

    @Test
    @DisplayName("알림에 batteryId 포함")
    void detect_alertContainsBatteryId() {
        List<AlertEvent> alerts = service.detect(event(65.0, 400.0));
        assertThat(alerts.get(0).getBatteryId()).isEqualTo("BAT-TEST");
    }

    @Test
    @DisplayName("전압 경계값 300V — 정상")
    void detect_voltageExactlyMin_noAlert() {
        List<AlertEvent> alerts = service.detect(event(35.0, 300.0));
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("전압 경계값 450V — 정상")
    void detect_voltageExactlyMax_noAlert() {
        List<AlertEvent> alerts = service.detect(event(35.0, 450.0));
        assertThat(alerts).isEmpty();
    }
}
