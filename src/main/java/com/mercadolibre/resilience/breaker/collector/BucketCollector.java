package com.mercadolibre.resilience.breaker.collector;

import com.mercadolibre.resilience.breaker.control.Consumer;
import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class BucketCollector implements Collector<Long> {

    private final ConcurrentMap<Long,Stats> measures = new ConcurrentHashMap<>();
    private final AtomicBoolean rolling = new AtomicBoolean(false);
    private final SortedSet<Long> timestamps = new ConcurrentSkipListSet<>();

    private final ExecutorService rollingPool = Executors.newSingleThreadExecutor();

    private final int maxSize;
    private final int staleInterval;
    private final long minMeasures;
    private final Consumer<List<Stats>> consumer;

    public BucketCollector(int maxSize, int staleInterval, long minMeasures, Consumer<List<Stats>> consumer) {
        this.maxSize = maxSize;
        this.staleInterval = staleInterval;
        this.minMeasures = minMeasures;
        this.consumer = consumer;
    }

    @Override
    public void collect(Long ts, boolean success) {
        Stats stats = checkStats(ts);

        if (success)
            stats.addSuccess();
        else
            stats.addFailure();
    }

    private Stats checkStats(long ts) {
        Stats stats = measures.putIfAbsent(ts, new SimpleStats());
        if (stats == null) {
            timestamps.add(ts);

            handleRoll();
            stats = measures.get(ts);
        }

        return stats;
    }

    private void handleRoll() {
        if (!rolling.compareAndSet(false,true)) return;

        if (measures.size() <= maxSize) {
            rolling.set(false);
            return;
        }

        rollingPool.submit(new Runnable() {
            public void run() {
                try {
                    roll();
                } finally {
                    rolling.set(false);
                }
            }
        });
    }

    private void roll() {
        long newest = timestamps.last();

        SortedSet<Long> buckets = timestamps.headSet(newest);
        List<Stats> output = new ArrayList<>(buckets.size());

        for (Long ts : buckets) {
            Stats stats = measures.get(ts);

            if (stats != null && !isStaled(newest, ts) && stats.count() >= minMeasures) output.add(stats);

            if (ts < newest - maxSize) measures.remove(ts);
        }

        timestamps.removeAll(buckets.headSet(buckets.last() - maxSize + 2));

        consumer.consume(output);
    }

    private boolean isStaled(long now, long ts) {
        return now > ts + staleInterval;
    }

    @Override
    public void shutdown() {
        rollingPool.shutdown();

        try {
            if (!rollingPool.awaitTermination(2, TimeUnit.SECONDS))
                rollingPool.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
