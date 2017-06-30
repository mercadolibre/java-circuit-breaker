package com.mercadolibre.resilience.breaker;

public interface Verifiable<T> {

    boolean isValid(T result, Throwable t);

}
