package com.mercadolibre.resilience.breaker;

import com.mercadolibre.resilience.breaker.control.CircuitControl;
import com.mercadolibre.resilience.breaker.control.OnOffCircuitControl;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class CircuitBreaker {

    protected static final int DEFAULT_PRECISION = 90;

    private volatile State state = State.CLOSED;

    private long interval;
    private long tryWindow;
    private CircuitControl control;
    private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> switchFuture;

    public static class Builder {
        private final CircuitBreaker breaker;
        private int precision = DEFAULT_PRECISION;
        private final AtomicBoolean built = new AtomicBoolean(false);

        private Builder() {
            this.breaker = new CircuitBreaker();
        }

        public Builder withInterval(long interval) {
            if (interval <= 0) throw new IllegalArgumentException("Interval should be positive");

            breaker.interval = interval;
            return this;
        }

        public Builder withTryWindow(long tryWindow) {
            if (tryWindow <= 0) throw new IllegalArgumentException("Try window should be positive");

            breaker.tryWindow = tryWindow;
            return this;
        }

        public Builder withPrecision(int precision) {
            if (precision <= 0) throw new IllegalArgumentException("Precision should be positive");

            this.precision = precision;
            return this;
        }

        public Builder withControl(CircuitControl control) {
            if (control == null) throw new IllegalArgumentException("Control should not be null");

            breaker.control = control;
            return this;
        }

        public CircuitBreaker build() {
            if (built.compareAndSet(false,true)) {
                breaker.switchFuture = breaker.pool.scheduleAtFixedRate(new Switch(breaker), 1000, precision, TimeUnit.MILLISECONDS);

                if (breaker.control == null) breaker.control = OnOffCircuitControl.builder().build();
            }

            return breaker;
        }
    }

    private CircuitBreaker() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public State getState() {
        return state;
    }

    protected void setState(State state) {
        this.state = state;
    }

    public long getInterval() {
        return interval;
    }

    public long getTryWindow() {
        return tryWindow;
    }

    protected CircuitControl getControl() {
        return control;
    }

    public void shutdown() {
        switchFuture.cancel(true);
    }

    public <T> T run(Action<T> action) throws RejectedExecutionException, ExecutionException {
        T output;

        switch (state) {
            case CLOSED:
            case HALF_OPEN:
                try {
                    output = action.get();
                    control.register(action, output);

                    return output;

                } catch (Exception e) {
                    control.register(action, e);

                    throw new ExecutionException(e);
                }

            case OPEN:
                throw new RejectedExecutionException("Breaker is open");

            default:
                throw new IllegalArgumentException("Unknown state " + state);
        }
    }

}

