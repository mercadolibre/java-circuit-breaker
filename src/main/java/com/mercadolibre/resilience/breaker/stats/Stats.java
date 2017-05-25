package com.mercadolibre.resilience.breaker.stats;

public abstract class Stats {

    public abstract long addSuccess();

    public abstract long addFailure();

    public abstract long successCount();

    public abstract long failureCount();

    public double failureRate() {
        long fails = failureCount();

        return fails * 1.0 / (fails + successCount());
    }

    public double successRate() {
        return 1 - failureRate();
    }

    public long count() {
        return successCount() + failureCount();
    }

}
