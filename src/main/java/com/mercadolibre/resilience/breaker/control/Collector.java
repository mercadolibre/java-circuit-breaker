package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.metrics.Metrics;
import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;


class Collector implements Runnable {

    public static final double[] DEFAULT_WEIGHTS = new double[]{0.1, 0.3, 0.6};

    private double[] weights = DEFAULT_WEIGHTS;

    private OnOffCircuitControl control;

    public static class Builder {
        private final Collector collector = new Collector();

        private Builder() {
        }

        public Builder withWeights(double[] weights) {
            if (weights == null || weights.length == 0 || weights.length > 120)
                throw new IllegalArgumentException("Weights must be an array of 1 to 120 elements");

            double sum = 0;
            for (double w : weights) {
                if (w < 0) throw new IllegalArgumentException("All elements must be non negative");

                sum += w;
            }

            if (sum == 0) throw new IllegalArgumentException("At least one weight must be positive") ;

            if (sum != 1) {
                double[] normalized = new double[weights.length];
                for (int i = 0; i < weights.length; i++) {
                    normalized[i] = weights[i] / sum;
                }

                collector.weights = normalized;
            } else
                collector.weights = weights;

            return this;
        }

        public Collector build() {
            return collector;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private Collector() {
    }

    private Long fromDate() {
        return control.getTimestamp() - weights.length;
    }

    protected double[] getWeights() {
        return weights;
    }

    protected void setControl(OnOffCircuitControl control) {
        this.control = control;
    }

    public void run() {
        ConcurrentNavigableMap<Long,Stats> chunk = control.getStats().tailMap(fromDate(), true);

        if (!chunk.isEmpty() && chunk.lastEntry().getValue().count() < control.getMinSampleSize())
            chunk.remove(chunk.lastKey());

        if (chunk.size() >= weights.length) {
            List<Stats> stats = new LinkedList<>(chunk.values());
            stats = stats.subList(stats.size() - weights.length, stats.size());

            double score = 0;

            for (int i = 0; i < weights.length; i++) {
                Stats s = stats.get(i);
                if (s.count() < control.getMinSampleSize()) return;

                score += weights[i] * s.successRate();
            }

            if (score > control.getMinScore()) {
                Metrics.INSTANCE.incrementCounter("resilience.breaker.close", 1);
                control.close();
            } else {
                Metrics.INSTANCE.incrementCounter("resilience.breaker.open", 1);
                control.open();
            }

        } else {
            Metrics.INSTANCE.incrementCounter("resilience.breaker.insufficient_data", 1);
        }

    }

}
