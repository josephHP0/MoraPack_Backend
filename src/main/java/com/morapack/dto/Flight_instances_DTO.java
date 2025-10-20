package com.morapack.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record Flight_instances_DTO (
        String instanceId,
        String flightId,
        String origin,
        String dest,
        String depUtc,
        String arrUtc,
        Integer capacity
){
}
