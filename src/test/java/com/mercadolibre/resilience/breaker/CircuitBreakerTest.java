package com.mercadolibre.resilience.breaker;

import com.mercadolibre.resilience.breaker.control.MockControl;
import org.junit.Test;

import static com.mercadolibre.resilience.breaker.TestUtil.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;


public class CircuitBreakerTest {

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileClosedWithOpeningControl() throws Exception {
        MockControl control = new MockControl();

        control.setOpen(true);

        CircuitBreaker breaker = new CircuitBreaker(1, 1, control);

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });

        assertTrue(result);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.OPEN, getAttribute(state, "state"));
        assertTrue((long) getAttribute(breaker, "openBegin") > 0);
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileClosedWithIdleControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });

        assertTrue(result);

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        assertEquals(State.CLOSED, getAttribute(state, "state"));
        assertNull(getAttribute(breaker, "openBegin"));
    }

    @Test(expected = ExecutionException.class)
    public void shouldReThrowActionWithException() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                throw new RuntimeException();
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });
    }

    @Test @SuppressWarnings("unchecked")
    public void shouldOperateWhileHalfOpenedAndClosingControl() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(1, 1, new MockControl());

        CircuitBreaker.BreakerState state = ((AtomicReference<CircuitBreaker.BreakerState>) getAttribute(breaker, "state")).get();

        setAttribute(state, "state", State.HALF_OPEN);

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });

        assertTrue(result);

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

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });

        assertTrue(result);

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

        boolean result = breaker.run(new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Throwable t) {
                return Boolean.TRUE.equals(result) && t == null;
            }
        });

        assertTrue(result);

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
            breaker.run(new Action<Boolean>() {
                @Override
                public Boolean get() throws Exception {
                    return true;
                }

                @Override
                public boolean isValid(Boolean result, Throwable t) {
                    return Boolean.TRUE.equals(result) && t == null;
                }
            });
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
            breaker.run(new Action<Boolean>() {
                @Override
                public Boolean get() throws Exception {
                    return true;
                }

                @Override
                public boolean isValid(Boolean result, Throwable t) {
                    return Boolean.TRUE.equals(result) && t == null;
                }
            });
        } catch (RejectedExecutionException e) {
            thrown = true;
        }

        assertTrue(thrown);
        assertNull(getAttribute(breaker, "openBegin"));
        assertNotNull(getAttribute(breaker, "halfOpenBegin"));
    }

}
