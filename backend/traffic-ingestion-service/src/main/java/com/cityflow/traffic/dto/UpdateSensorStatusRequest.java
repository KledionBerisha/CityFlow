package com.cityflow.traffic.dto;

import com.cityflow.traffic.model.SensorStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateSensorStatusRequest {

    @NotNull(message = "Status is required")
    private SensorStatus status;

    public UpdateSensorStatusRequest() {}

    public UpdateSensorStatusRequest(SensorStatus status) {
        this.status = status;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public void setStatus(SensorStatus status) {
        this.status = status;
    }
}
