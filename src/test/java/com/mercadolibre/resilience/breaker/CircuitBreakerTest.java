package com.mercadolibre.resilience.breaker;

import com.mercadolibre.resilience.breaker.control.CircuitControl;
import com.mercadolibre.resilience.breaker.control.OnOffCircuitControl;
import com.mercadolibre.resilience.breaker.util.TestUtil;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;


public class CircuitBreakerTest {

    @Test
    public void shouldWorkWhileClosed() throws ExecutionException {
        final List<Object> actions = Collections.synchronizedList(new LinkedList<>());

        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {
                actions.add(action);
            }

            @Override
            public <T> void register(Action<T> action, Exception e) {
                actions.add(e);
            }

            @Override
            public boolean shouldOpen() {
                return false;
            }

            @Override
            public boolean shouldClose() {
                return true;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1000)
                .withTryWindow(1000)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        assertEquals(State.CLOSED, breaker.getState());

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return true;
            }
        });

        assertTrue(result);

        Exception t = null;

        try {
            breaker.run(new Action<Boolean>() {
                @Override
                public Boolean get() throws Exception {
                    throw new RuntimeException();
                }

                @Override
                public boolean isValid(Boolean result, Exception e) {
                    return false;
                }
            });
        } catch (ExecutionException e) {
            t = e;
        }

        assertNotNull(t);

        assertEquals(2, actions.size());

        assertTrue(actions.get(0) instanceof Action);
        assertTrue(actions.get(1) instanceof RuntimeException);
    }


    @Test
    public void shouldWorkWhileHalfOpen() throws ExecutionException {
        final List<Object> actions = Collections.synchronizedList(new LinkedList<>());

        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {
                actions.add(action);
            }

            @Override
            public <T> void register(Action<T> action, Exception e) {
                actions.add(e);
            }

            @Override
            public boolean shouldOpen() {
                return false;
            }

            @Override
            public boolean shouldClose() {
                return true;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1000)
                .withTryWindow(1000)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        breaker.setState(State.HALF_OPEN);

        assertEquals(State.HALF_OPEN, breaker.getState());

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return true;
            }
        });

        assertTrue(result);

        Exception t = null;

        try {
            breaker.run(new Action<Boolean>() {
                @Override
                public Boolean get() throws Exception {
                    throw new RuntimeException();
                }

                @Override
                public boolean isValid(Boolean result, Exception e) {
                    return false;
                }
            });
        } catch (ExecutionException e) {
            t = e;
        }

        assertNotNull(t);

        assertEquals(2, actions.size());

        assertTrue(actions.get(0) instanceof Action);
        assertTrue(actions.get(1) instanceof RuntimeException);

        assertEquals(State.HALF_OPEN, breaker.getState());
    }

    @Test(expected = RejectedExecutionException.class)
    public void shouldWorkWhileOpen() throws ExecutionException {
        final List<Object> actions = Collections.synchronizedList(new LinkedList<>());

        CircuitControl control = new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {
                actions.add(action);
            }

            @Override
            public <T> void register(Action<T> action, Exception e) {
                actions.add(e);
            }

            @Override
            public boolean shouldOpen() {
                return false;
            }

            @Override
            public boolean shouldClose() {
                return true;
            }
        };

        CircuitBreaker breaker = CircuitBreaker
                .builder()
                .withControl(control)
                .withInterval(1000)
                .withTryWindow(1000)
                .withPrecision(Integer.MAX_VALUE)
                .build();

        breaker.setState(State.OPEN);

        assertEquals(State.OPEN, breaker.getState());

        breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return true;
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveInterval() {
        CircuitBreaker.builder().withControl(dummyControl()).withInterval(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveTryWindow() {
        CircuitBreaker.builder().withControl(dummyControl()).withTryWindow(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositivePrecision() {
        CircuitBreaker.builder().withControl(dummyControl()).withPrecision(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullControl() {
        CircuitBreaker.builder().withControl(null);
    }

    @Test
    public void shouldBuildOnlyOnce() {
        CircuitBreaker.Builder builder = CircuitBreaker.builder().withControl(dummyControl());
        CircuitBreaker breaker = builder.build();

        ScheduledFuture<?> future = (ScheduledFuture<?>) TestUtil.getAttribute("switchFuture", breaker);

        assertNotNull(future);

        assertTrue(breaker == builder.build());
        assertTrue(future == TestUtil.getAttribute("switchFuture", breaker));
    }

    @Test
    public void shouldBuildWithDefaultControl() {
        CircuitBreaker breaker = CircuitBreaker.builder().build();

        assertNotNull(breaker);
        assertNotNull(breaker.getControl());
        assertTrue(breaker.getControl() instanceof OnOffCircuitControl);
    }

    @Test
    public void shouldShutdown() {
        CircuitBreaker breaker = CircuitBreaker.builder().withControl(dummyControl()).build();

        breaker.shutdown();

        assertTrue(((ScheduledFuture<?>) TestUtil.getAttribute("switchFuture", breaker)).isCancelled());
    }

    private CircuitControl dummyControl() {
        return new CircuitControl() {
            @Override
            public <T> void register(Action<T> action, T data) {
            }

            @Override
            public <T> void register(Action<T> action, Exception e) {
            }

            @Override
            public boolean shouldOpen() {
                return false;
            }

            @Override
            public boolean shouldClose() {
                return true;
            }
        };
    }

}
