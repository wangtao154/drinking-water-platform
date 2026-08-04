package com.platform.ai.model;

import lombok.Data;

@Data
public class MetricStats {

    private final String field;
    private int count;
    private Double first;
    private Double last;
    private Double min;
    private Double max;
    private double sum;

    public MetricStats(String field) {
        this.field = field;
    }

    public void add(double value) {
        if (count == 0) {
            first = value;
            min = value;
            max = value;
        } else {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        last = value;
        sum += value;
        count++;
    }

    public Double getAvg() {
        return count == 0 ? null : sum / count;
    }

    public Double getDelta() {
        if (first == null || last == null) {
            return null;
        }
        return last - first;
    }
}
