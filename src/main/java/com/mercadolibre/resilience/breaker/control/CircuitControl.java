package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.Action;

public interface CircuitControl {

    <T> void register(Action<T> action, T data);

    <T> void register(Action<T> action, Exception e);

    boolean shouldOpen();

    boolean shouldClose();

}
