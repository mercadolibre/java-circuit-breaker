package com.mercadolibre.resilience.breaker;

import com.mercadolibre.common.async.Callback;
import com.mercadolibre.resilience.breaker.control.MockControl;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mercadolibre.resilience.breaker.TestUtil.getAttribute;
import static com.mercadolibre.resilience.breaker.TestUtil.setAttribute;
import static org.junit.Assert.*;


public class CircuitBreakerAsyncTest {

    private static class SimpleCallback implements Callback<Boolean> {
        private final CountDownLatch latch;
        private final AtomicBoolean result;
        private final AtomicReference<Throwable> tReference;

        public SimpleCallback(CountDownLatch latch, AtomicBoolean result, AtomicReference<Throwable> tReference) {
            this.latch = latch;
            this.result = result;
            this.tReference = tReference;
        }

        @Override
        public void success(Boolean response) {
            result.set(response);
            latch.countDown();
        }

        @Override
        public void failure(Throwable t) {
            tReference.set(t);
            latch.countDown();
        }

        @Override
        public void cancel() {

        }
    }

    private void runAsync(CircuitBreaker breaker, final Class<? extends Throwable> expected) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        AtomicReference<Throwable> tReference = new AtomicReference<>();

        breaker.run(new AsyncAction<Boolean, Object>() {
            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }

            @Override
            public Object get(Callback<Boolean> callback) {
                if (expected == null)
                    callback.success(true);
                else try {
                    callback.failure(expected.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    callback.failure(e);
                }

                return null;
            }
        }, new SimpleCallback(latch, result, tReference));

        latch.await();

        if (expected == null) {
            assertTrue(result.get());
            assertNull(tReference.get());
        } else {
            assertFalse(result.get());
            assertTrue(expected.isInstance(tReference.get()));
        }
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileClosedWithOpeningControl() throws Exception {
        MockControl control = new MockControl();

        control.setOpen(true);

        CircuitBreaker breaker = new CircuitBreaker(1, 1, control);

        runAsync(breaker, null);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.OPEN, getAttribute(state, "state"));
        assertTrue((long) getAttribute(breaker, "openBegin") > 0);
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileClosedWithIdleControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        runAsync(breaker, null);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.CLOSED, getAttribute(state, "state"));
        assertNull(getAttribute(breaker, "openBegin"));
    }

    @Test
    public void shouldReThrowActionWithException() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        runAsync(breaker, RuntimeException.class);
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileHalfOpenedAndClosingControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.HALF_OPEN);

        runAsync(breaker, null);

        state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.CLOSED, getAttribute(state, "state"));
        assertNull(getAttribute(breaker, "halfOpenBegin"));
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileHalfOpenedAndIdleControl() throws Exception {
        MockControl control = new MockControl();

        control.setOpen(true);

        CircuitBreaker breaker = new CircuitBreaker(1, 1, control);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.HALF_OPEN);
        setAttribute(breaker, "halfOpenBegin", System.currentTimeMillis() + 10000);

        runAsync(breaker, null);

        state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.HALF_OPEN, getAttribute(state, "state"));
        assertNotNull(getAttribute(breaker, "halfOpenBegin"));
        assertNull(getAttribute(breaker, "openBegin"));
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileHalfOpenedAndOpeningControl() throws Exception {
        MockControl control = new MockControl();

        control.setOpen(true);

        CircuitBreaker breaker = new CircuitBreaker(1, 1, control);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.HALF_OPEN);
        setAttribute(breaker, "halfOpenBegin", 0L);

        runAsync(breaker, null);

        state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.OPEN, getAttribute(state, "state"));
        assertNull(getAttribute(breaker, "halfOpenBegin"));
        assertTrue((long) getAttribute(breaker, "openBegin") > 0);
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldRejectWhileOpenAndIdleControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.OPEN);
        setAttribute(breaker, "openBegin", System.currentTimeMillis() + 10000);

        boolean thrown = false;

        try {
            runAsync(breaker, null);
        } catch (RejectedExecutionException e) {
            thrown = true;
        }

        assertTrue(thrown);
        assertNotNull(getAttribute(breaker, "openBegin"));
        assertNull(getAttribute(breaker, "halfOpenBegin"));
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldRejectWhileOpenAndHalfOpeningControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.OPEN);
        setAttribute(breaker, "openBegin", 0L);

        boolean thrown = false;

        try {
            runAsync(breaker, null);
        } catch (RejectedExecutionException e) {
            thrown = true;
        }

        assertTrue(thrown);
        assertNull(getAttribute(breaker, "openBegin"));
        assertNotNull(getAttribute(breaker, "halfOpenBegin"));
    }

}
