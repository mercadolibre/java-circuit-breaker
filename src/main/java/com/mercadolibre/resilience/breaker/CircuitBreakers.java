package com.mercadolibre.resilience.breaker;

import com.mercadolibre.resilience.breaker.control.CircuitControl;
import com.mercadolibre.resilience.breaker.control.ExponentialControl;
import com.mercadolibre.resilience.breaker.control.FixedWeightsControl;

public class CircuitBreakers {

    public static final long DEFAULT_BUCKET_WIDTH_MS = 1000;

    public static CircuitBreaker newFixedWeightsBreaker(long interval, long tryWindow, double[] weights, double minScore,
                                                     int staleInterval, long bucketWidthMs, int minMeasures) {
        CircuitControl<Long> control = new FixedWeightsControl(weights, minScore, staleInterval, bucketWidthMs, minMeasures);

        return new CircuitBreaker(interval, tryWindow, control);
    }

    public static CircuitBreaker newFixedWeightsBreaker(long interval, double[] weights, double minScore, int minMeasures) {
        return newFixedWeightsBreaker(interval, weights.length * DEFAULT_BUCKET_WIDTH_MS, weights, minScore, weights.length, DEFAULT_BUCKET_WIDTH_MS, minMeasures);
    }

    public static CircuitBreaker newExponentialBreaker(long interval, long tryWindow, double coefficient, int buckets,
                                                       long bucketWidthMs, double minScore, int staleInterval, int minMeasures) {
        CircuitControl<Long> control = new ExponentialControl(coefficient, buckets, minScore, staleInterval, bucketWidthMs, minMeasures);

        return new CircuitBreaker(interval, tryWindow, control);
    }

    public static CircuitBreaker newExponentialBreaker(long interval, double coefficient, int buckets,
                                                       double minScore, int minMeasures) {
        return newExponentialBreaker(interval, buckets * DEFAULT_BUCKET_WIDTH_MS, coefficient, buckets, DEFAULT_BUCKET_WIDTH_MS, minScore, buckets, minMeasures);
    }

    public static CircuitBreaker newDefaultBreaker(long interval) {
        return newExponentialBreaker(interval, 0.1, 10, 0.8, 10);
    }

}
