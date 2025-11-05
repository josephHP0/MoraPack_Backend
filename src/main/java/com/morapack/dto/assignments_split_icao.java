package com.morapack.dto;

import java.util.List;
import lombok.Data;


@Data
public class assignments_split_icao {
    private String orderId;
    private List<Split> splits;

    @Data
    public static class Split {
        private String consignmentId;
        private Integer qty;
        private List<Leg> legs;
    }

    @Data
    public static class Leg {
        private Integer seq;
        private String instanceId;
        private String from;
        private String to;
        private Integer qty;
    }
}