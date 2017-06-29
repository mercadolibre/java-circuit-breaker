package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.List;


public class FixedWeightsControl extends BasicBucketControl {

    private final double[] weights;

    public FixedWeightsControl(double[] weights, double minScore, int staleInterval, long bucketWidthMs, int minMeasures) {
        super(weights.length, minScore, staleInterval, bucketWidthMs, minMeasures);

        this.weights = normalize(weights);
    }

    private double[] normalize(double[] weights) {
        double sum = 0;

        for (double w : weights) sum += w;

        if (sum != 0 && sum != 1) {
            double[] output = new double[weights.length];
            for (int i = 0; i < weights.length; i++)
                output[i] = weights[i] / sum;

            return output;
        }

        return weights;
    }

    @Override
    protected double score(List<Stats> stats) {
        double score = 0;

        for (int i = 0; i < buckets; i++)
            score += stats.get(i).successRate() * weights[i];

        return score;
    }

}
