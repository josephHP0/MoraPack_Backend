package com.morapack.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PlanResponse {
    private String orderId;
    private List<Split> splits;

    @Setter
    @Getter
    public static class Split {
        private String consignmentId;
        private int qty;
        private List<Leg> legs;

    }

    @Setter
    @Getter
    public static class Leg {
        private int seq;
        private String instanceId;
        private String from;
        private String to;
        private int qty;

    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public void setSplits(List<Split> splits) {
        this.splits = splits;
    }
}