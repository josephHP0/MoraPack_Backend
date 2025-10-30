package com.morapack.dto;

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