package com.morapack.simulador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TimelineStore {
    private List<Event> events = new ArrayList<>();
    private String version;

    public void load(PlanResult plan, List<Event> timeline) {
        this.events = new ArrayList<>(timeline);
        this.version = java.util.UUID.randomUUID().toString();
    }

    public String getVersion() {
        return version;
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public List<Event> eventsBetween(Instant from, Instant to) {
        return events.stream()
                .filter(e -> !e.getTime().isBefore(from) && !e.getTime().isAfter(to))
                .collect(Collectors.toList());
    }
}