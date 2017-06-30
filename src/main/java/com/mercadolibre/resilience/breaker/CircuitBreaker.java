package com.mercadolibre.resilience.breaker;

import static com.mercadolibre.resilience.breaker.State.*;

import com.mercadolibre.common.async.Callback;
import com.mercadolibre.metrics.Metrics;
import com.mercadolibre.resilience.breaker.control.CircuitControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


public class CircuitBreaker {

    static class BreakerState {
        private State state;
        private long epoch;

        private BreakerState(State state, long epoch) {
            this.state = state;
            this.epoch = epoch;
        }

        private BreakerState next(State state) {
            return new BreakerState(state, epoch + 1);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final AtomicReference<BreakerState> state = new AtomicReference<>(new BreakerState(CLOSED, 0));

    private final long interval;
    private final long tryWindow;
    private Long openBegin;
    private Long halfOpenBegin;
    private final CircuitControl<?> control;


    public CircuitBreaker(long interval, long tryWindow, CircuitControl<?> control) {
        this.interval = interval;
        this.tryWindow = tryWindow;
        this.control = control;
    }

    private void checkShouldOpen(BreakerState breakerState) {
        if (control.shouldOpen() && state.compareAndSet(breakerState, breakerState.next(OPEN))) {
            openBegin = System.currentTimeMillis();
            Metrics.INSTANCE.count("resilience.breaker.open", 1);
            log.debug("CLOSED -> OPEN");
        }
    }

    private void checkShouldHalfOpen(BreakerState breakerState) {
        long now = System.currentTimeMillis();
        if (now >= interval + openBegin && state.compareAndSet(breakerState, breakerState.next(HALF_OPEN))) {
            halfOpenBegin = now;
            openBegin = null;
            Metrics.INSTANCE.count("resilience.breaker.half_open", 1);
            log.debug("OPEN -> HALF_OPEN");
        }
    }

    @SuppressWarnings("SingleStatementInBlock")
    private void checkShouldClose(BreakerState breakerState) {
        if (control.shouldClose()) {
            if (state.compareAndSet(breakerState, breakerState.next(CLOSED))) {
                halfOpenBegin = null;
                Metrics.INSTANCE.count("resilience.breaker.close", 1);
                log.debug("HALF_OPEN -> CLOSED");
            }
        } else {
            long now = System.currentTimeMillis();
            if (now >= tryWindow + halfOpenBegin && state.compareAndSet(breakerState, breakerState.next(OPEN))) {
                openBegin = now;
                halfOpenBegin = null;
                Metrics.INSTANCE.count("resilience.breaker.open", 1);
                log.debug("HALF_OPEN -> OPEN");
            }
        }
    }

    private void doCheck(BreakerState breakerState) {
        switch (breakerState.state) {
            case CLOSED: checkShouldOpen(breakerState); break;
            case HALF_OPEN: checkShouldClose(breakerState); break;
            default: throw new IllegalStateException("Cannot check state " + breakerState.state);
        }
    }

    private <T> T runAction(BreakerState breakerState, Action<T> action) throws ExecutionException {
        T output;
        try {
            output = action.get();
            control.register(action, output);
            registerMetric(action, output, null);

            return output;
        } catch (Exception e) {
            control.register(action, e);
            registerMetric(action, null, e);

            throw new ExecutionException(e);
        } finally {
            doCheck(breakerState);
        }
    }

    private <T> void registerMetric(Verifiable<T> action, T result, Throwable t) {
        if (action.isValid(result, t))
            Metrics.INSTANCE.count("resilience.breaker.success", 1);
        else
            Metrics.INSTANCE.count("resilience.breaker.fail", 1);
    }

    private <T> void register(AsyncAction<T,?> action, T response, BreakerState breakerState) {
        try {
            control.register(action, response);
            registerMetric(action, response, null);
            doCheck(breakerState);
        } catch (Exception e) {
            log.error("Could not register action", e);
        }
    }

    private <T> void register(AsyncAction<T,?> action, Throwable t, BreakerState breakerState) {
        try {
            control.register(action, t);
            registerMetric(action, null, t);
            doCheck(breakerState);
        } catch (Exception e) {
            log.error("Could not register action in breaker", e);
        }
    }

    public void shutdown() {
        control.shutdown();
    }

    public <T> T run(Action<T> action) throws ExecutionException {
        final BreakerState breakerState = state.get();

        switch (breakerState.state) {
            case CLOSED:
            case HALF_OPEN:
                return runAction(breakerState, action);
            case OPEN:
                checkShouldHalfOpen(breakerState);
                throw new RejectedExecutionException("Breaker is open");
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    }

    public <T,R> R run(final AsyncAction<T,R> action, final Callback<T> callback) {
        final BreakerState breakerState = state.get();

        switch (breakerState.state) {
            case CLOSED:
            case HALF_OPEN:
                return action.get(new Callback<T>() {
                    @Override
                    public void success(T response) {
                        register(action, response, breakerState);

                        callback.success(response);
                    }

                    @Override
                    public void failure(Throwable t) {
                        register(action, t, breakerState);

                        callback.failure(t);
                    }

                    @Override
                    public void cancel() {
                        callback.cancel();
                    }
                });
            case OPEN:
                checkShouldHalfOpen(breakerState);
                throw new RejectedExecutionException("Breaker is open");
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    }

}

