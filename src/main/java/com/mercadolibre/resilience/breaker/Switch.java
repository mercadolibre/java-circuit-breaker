package com.mercadolibre.resilience.breaker;


class Switch implements Runnable {

    private final CircuitBreaker breaker;

    private Long openBegin;
    private Long halfOpenBegin;

    protected Switch(CircuitBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public void run() {
        switch (breaker.getState()) {
            case CLOSED:
                checkShouldOpen(); break;

            case OPEN:
                checkShouldHalfOpen(); break;

            case HALF_OPEN:
                checkShouldClose(); break;

            default: break;
        }
    }

    private void checkShouldOpen() {
        if (breaker.getControl().shouldOpen()) {
            breaker.setState(State.OPEN);
            openBegin = System.currentTimeMillis();
        }
    }

    private void checkShouldHalfOpen() {
        long now = System.currentTimeMillis();
        if (now - openBegin >= breaker.getInterval()) {
            halfOpenBegin = now;
            breaker.setState(State.HALF_OPEN);
            openBegin = null;
        }
    }

    private void checkShouldClose() {
        if (breaker.getControl().shouldClose()) {
            breaker.setState(State.CLOSED);
            halfOpenBegin = null;
        } else if (System.currentTimeMillis() - halfOpenBegin >= breaker.getTryWindow()) {
            breaker.setState(State.OPEN);
            openBegin = System.currentTimeMillis();
            halfOpenBegin = null;
        }
    }

}
