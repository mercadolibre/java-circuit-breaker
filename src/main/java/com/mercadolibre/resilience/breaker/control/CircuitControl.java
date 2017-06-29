package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.Verifiable;
import com.mercadolibre.resilience.breaker.collector.Collector;


public abstract class CircuitControl<K> {

    public <T> void register(Verifiable<T> action, T data) {
        boolean result = action.isValid(data, null);
        collector().collect(getKey(), result);
    }

    public <T> void register(Verifiable<T> action, Throwable t) {
        boolean result = action.isValid(null, t);
        collector().collect(getKey(), result);
    }

    protected abstract Collector<K> collector();

    protected abstract K getKey();

    public abstract boolean shouldOpen();

    public boolean shouldClose() {
        return !shouldOpen();
    }

    public void shutdown() {
        collector().shutdown();
    }

}
