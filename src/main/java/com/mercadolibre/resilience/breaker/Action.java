package com.mercadolibre.resilience.breaker;

public interface Action<T> {

    T get() throws Exception;

    boolean isValid(T result, Exception e);

}
