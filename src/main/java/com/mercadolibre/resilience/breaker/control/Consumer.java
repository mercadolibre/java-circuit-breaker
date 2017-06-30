package com.mercadolibre.resilience.breaker.control;

public interface Consumer<T> {

    void consume(T t);

}
