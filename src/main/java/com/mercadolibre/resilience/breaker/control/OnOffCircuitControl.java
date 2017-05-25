package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.metrics.Metrics;
import com.mercadolibre.resilience.breaker.Action;
import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class OnOffCircuitControl implements CircuitControl {

    public static final double DEFAULT_MIN_SCORE = 0.6;
    public static final long DEFAULT_MIN_SAMPLE_SIZE = 50;
    public static final long DEFAULT_COLLECTOR_THREAD_DELAY = 10*1000;
    public static final long DEFAULT_COLLECTOR_THREAD_INTERVAL = 1*1000;
    public static final long DEFAULT_CLEANER_THREAD_DELAY = 20*1000;
    public static final long DEFAULT_CLEANER_THREAD_INTERVAL = 20*1000;

    private double minScore = DEFAULT_MIN_SCORE;
    private long minSampleSize = DEFAULT_MIN_SAMPLE_SIZE;

    private final ConcurrentNavigableMap<Long,Stats> stats = new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean closed = true;

    private ScheduledFuture<?> collectorFuture;
    private ScheduledFuture<?> cleanerFuture;

    private Collector collector;

    public static final class Builder {
        private final OnOffCircuitControl control;
        private boolean startWorkers = true;
        private long collectorThreadDelay = DEFAULT_COLLECTOR_THREAD_DELAY;
        private long collectorThreadInterval = DEFAULT_COLLECTOR_THREAD_INTERVAL;
        private long cleanerThreadDelay = DEFAULT_CLEANER_THREAD_DELAY;
        private long cleanerThreadInterval = DEFAULT_CLEANER_THREAD_INTERVAL;
        private final AtomicBoolean built = new AtomicBoolean(false);

        private Builder() {
            this.control = new OnOffCircuitControl();
        }

        public Builder withMinScore(double minScore) {
            if (minScore < 0) throw new IllegalArgumentException("minScore must be non negative");

            control.minScore = minScore;
            return this;
        }

        public Builder withMinSampleSize(long minSampleSize) {
            if (minSampleSize < 0) throw new IllegalArgumentException("minSampleSize must be non negative");

            control.minSampleSize = minSampleSize;
            return this;
        }

        public Builder startWorkers(boolean startWorkers) {
            this.startWorkers = startWorkers;
            return this;
        }

        public Builder withCollector(Collector collector) {
            if (collector == null) throw new IllegalArgumentException("Collector should not be null");

            control.collector = collector;
            return this;
        }

        public Builder withCollectorThreadDelay(long collectorThreadDelay) {
            if (collectorThreadDelay < 0) throw new IllegalArgumentException("collectorThreadDelay must be non negative");

            this.collectorThreadDelay = collectorThreadDelay;
            return this;
        }

        public Builder withCollectorThreadInterval(long collectorThreadInterval) {
            if (collectorThreadInterval <= 0) throw new IllegalArgumentException("collectorThreadInterval must be positive");

            this.collectorThreadInterval = collectorThreadInterval;
            return this;
        }

        public Builder withCleanerThreadDelay(long cleanerThreadDelay) {
            if (cleanerThreadDelay < 0) throw new IllegalArgumentException("cleanerThreadDelay must be non negative");

            this.cleanerThreadDelay = cleanerThreadDelay;
            return this;
        }

        public Builder withCleanerThreadInterval(long cleanerThreadInterval) {
            if (cleanerThreadInterval <= 0) throw new IllegalArgumentException("cleanerThreadInterval must be positive");

            this.cleanerThreadInterval = cleanerThreadInterval;
            return this;
        }

        public OnOffCircuitControl build() {
            if (!built.compareAndSet(false,true)) return control;

            if (control.collector == null) control.collector = Collector.builder().build();

            control.collector.setControl(control);

            if (startWorkers) control.scheduleWorkers(collectorThreadDelay, collectorThreadInterval, cleanerThreadDelay, cleanerThreadInterval);

            return control;
        }
    }

    private OnOffCircuitControl() {
    }

    public static Builder builder() {
        return new Builder();
    }

    private void scheduleWorkers(long collectorDelay, long collectorInterval, long cleanerDelay, long cleanerInterval) {
        collectorFuture = pool.scheduleAtFixedRate(collector, collectorDelay, collectorInterval, TimeUnit.MILLISECONDS);
        cleanerFuture = pool.scheduleAtFixedRate(new Cleaner(this), cleanerDelay, cleanerInterval, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (collectorFuture != null) collectorFuture.cancel(true);
        if (cleanerFuture != null) cleanerFuture.cancel(true);
    }

    protected long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    private void registerData(boolean result) {
        long key = getTimestamp();

        if (!stats.containsKey(key))
            stats.putIfAbsent(key, new SimpleStats());

        if (result) {
            stats.get(key).addSuccess();
            Metrics.INSTANCE.incrementCounter("resilience.breaker.success", 1);
        } else {
            stats.get(key).addFailure();
            Metrics.INSTANCE.incrementCounter("resilience.breaker.fail", 1);
        }
    }

    @Override
    public <T> void register(Action<T> action, T data) {
        boolean result = action.isValid(data, null);
        registerData(result);
    }

    @Override
    public <T> void register(Action<T> action, Exception e) {
        boolean result = action.isValid(null, e);
        registerData(result);
    }

    @Override
    public boolean shouldOpen() {
        return !closed;
    }

    @Override
    public boolean shouldClose() {
        return closed;
    }

    protected void open() {
        closed = false;
    }

    protected void close() {
        closed = true;
    }

    protected Collector getCollector() {
        return collector;
    }

    public double getMinScore() {
        return minScore;
    }

    public long getMinSampleSize() {
        return minSampleSize;
    }

    protected ConcurrentNavigableMap<Long, Stats> getStats() {
        return stats;
    }
}
