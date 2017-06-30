package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.collector.BucketCollector;
import com.mercadolibre.resilience.breaker.collector.Collector;
import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.List;


public abstract class BasicBucketControl extends CircuitControl<Long> {

    private final BucketCollector collector;

    protected final int buckets;
    protected final double minScore;
    protected final long bucketWidthMs;

    private volatile boolean open = false;

    protected BasicBucketControl(final int buckets, double minScore, int staleInterval, long bucketWidthMs, int minMeasures) {
        if (buckets <= 0) throw new IllegalArgumentException("Bucket amount must be positive");
        if (minScore < 0 || minScore > 1) throw new IllegalArgumentException("Min score must lay in [0,1] interval");
        if (staleInterval < 0) throw new IllegalArgumentException("Stale interval must be non negative");
        if (bucketWidthMs <= 0) throw new IllegalArgumentException("Bucket width must be positive");
        if (minMeasures < 0) throw new IllegalArgumentException("Min measures must be non negative");

        this.buckets = buckets;
        this.minScore = minScore;
        this.bucketWidthMs = bucketWidthMs;

        collector = new BucketCollector(buckets, staleInterval, minMeasures, new Consumer<List<Stats>>() {
            public void consume(List<Stats> stats) {
                if (stats.size() >= buckets) doSwitch(score(stats));
            }
        });
    }

    protected abstract double score(List<Stats> stats);

    private void doSwitch(double score) {
        open = score < minScore;
    }

    @Override
    protected final Long getKey() {
        return System.currentTimeMillis() / bucketWidthMs;
    }

    @Override
    protected final Collector<Long> collector() {
        return collector;
    }

    @Override
    public final boolean shouldOpen() {
        return open;
    }

}
