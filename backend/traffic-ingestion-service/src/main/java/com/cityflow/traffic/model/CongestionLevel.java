package com.cityflow.traffic.model;

public enum CongestionLevel {
    FREE_FLOW,      // < 30% capacity, speed > 80% of limit
    LIGHT,          // 30-50% capacity, speed 60-80% of limit
    MODERATE,       // 50-70% capacity, speed 40-60% of limit
    HEAVY,          // 70-90% capacity, speed 20-40% of limit
    SEVERE          // > 90% capacity, speed < 20% of limit
}
