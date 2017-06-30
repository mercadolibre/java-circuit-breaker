package com.mercadolibre.resilience.breaker.collector;

public enum DummyCollector implements Collector<Long> {
    INSTANCE;

    @Override
    public void collect(Long key, boolean success) {

    }

    @Override
    public void shutdown() {

    }
}
