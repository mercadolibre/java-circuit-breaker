package com.mercadolibre.resilience.breaker.collector;

public interface Collector<T> {

    void collect(T key, boolean success);

    void shutdown();

}
