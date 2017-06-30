package com.mercadolibre.resilience.breaker;

public interface Action<T> extends Verifiable<T> {

    T get() throws Exception;

}
