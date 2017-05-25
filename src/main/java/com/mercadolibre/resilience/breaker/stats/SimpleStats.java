package com.mercadolibre.resilience.breaker.stats;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleStats extends Stats {

    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    @Override
    public long addSuccess() {
       return successes.incrementAndGet();
    }

    @Override
    public long addFailure() {
        return failures.incrementAndGet();
    }

    @Override
    public long successCount() {
        return successes.get();
    }

    @Override
    public long failureCount() {
        return failures.get();
    }

}
