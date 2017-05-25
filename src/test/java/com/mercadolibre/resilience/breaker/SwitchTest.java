package com.mercadolibre.resilience.breaker;

import com.mercadolibre.resilience.breaker.control.CircuitControl;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

public class SwitchTest {

    @Test
    public void shouldTransitionClosedToOpen() {

        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return true;
            }

            @Override
            public boolean shouldClose() {
                return false;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(10000)
                .withTryWindow(10000)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        new Switch(breaker).run();

        assertEquals(State.OPEN, breaker.getState());
    }

    @Test
    public void shouldTransitionOpenToHalfOpen() {
        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return true;
            }

            @Override
            public boolean shouldClose() {
                return false;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1)
                .withTryWindow(1)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        Switch sw = new Switch(breaker);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());

        LockSupport.parkNanos(2000000);

        sw.run();

        assertEquals(State.HALF_OPEN, breaker.getState());
    }

    @Test
    public void shouldTransitionHalfOpenToClosed() {
        final AtomicBoolean close = new AtomicBoolean(false);

        CircuitControl control = new CircuitControl() {

            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return !close.get();
            }

            @Override
            public boolean shouldClose() {
                return close.get();
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1)
                .withTryWindow(100)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        Switch sw = new Switch(breaker);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());

        LockSupport.parkNanos(2000000);

        sw.run();

        assertEquals(State.HALF_OPEN, breaker.getState());

        close.set(true);

        sw.run();

        assertEquals(State.CLOSED, breaker.getState());
    }

    @Test
    public void shouldNotTransitionToHalfOpenWithoutExpiration() {
        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return true;
            }

            @Override
            public boolean shouldClose() {
                return false;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(Integer.MAX_VALUE)
                .withTryWindow(1)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        Switch sw = new Switch(breaker);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());

        sw.run();

        assertEquals(State.OPEN, breaker.getState());
    }

    @Test
    public void shouldOpenFromHalfOpenIfWindowExpired() {

        CircuitControl control = new CircuitControl() {

            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return true;
            }

            @Override
            public boolean shouldClose() {
                return false;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1)
                .withTryWindow(1)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        Switch sw = new Switch(breaker);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());

        LockSupport.parkNanos(2000000);

        sw.run();

        assertEquals(State.HALF_OPEN, breaker.getState());

        LockSupport.parkNanos(2000000);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());
    }

    @Test
    public void shouldNotFromHalfOpenIfWindowActive() {

        CircuitControl control = new CircuitControl() {

            @Override
            public <T> void register(Action<T> action, T data) {

            }

            @Override
            public <T> void register(Action<T> action, Exception e) {

            }

            @Override
            public boolean shouldOpen() {
                return true;
            }

            @Override
            public boolean shouldClose() {
                return false;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1)
                .withTryWindow(100)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        Switch sw = new Switch(breaker);

        sw.run();

        assertEquals(State.OPEN, breaker.getState());

        LockSupport.parkNanos(2000000);

        sw.run();

        assertEquals(State.HALF_OPEN, breaker.getState());

        sw.run();

        assertEquals(State.HALF_OPEN, breaker.getState());
    }

}
