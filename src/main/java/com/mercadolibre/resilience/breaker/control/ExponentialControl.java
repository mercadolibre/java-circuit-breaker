package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.List;


public class ExponentialControl extends BasicBucketControl {

    private final double coefficient;
    private final double conjCoefficient;

    public ExponentialControl(double coefficient, int windowSize, double minScore, int staleInterval, long bucketWidthMs, int minMeasures) {
        super(windowSize, minScore, staleInterval, bucketWidthMs, minMeasures);

        if (coefficient < 0 || coefficient > 1) throw new IllegalArgumentException("Coefficient must lay in [0,1] interval");

        this.coefficient = coefficient;
        this.conjCoefficient = 1 - coefficient;
    }

    @Override
    protected double score(List<Stats> stats) {
        double score = 0;

        int n = stats.size() - 1;

        for (int i = 0; i < buckets; i++)
            score = coefficient * score + conjCoefficient * stats.get(n-i).successRate();

        return score;
    }

}
