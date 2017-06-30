package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.collector.Collector;
import com.mercadolibre.resilience.breaker.collector.DummyCollector;


public class MockControl extends CircuitControl<Long> {

    private Collector<Long> collector = DummyCollector.INSTANCE;
    private Long key = 0L;
    private boolean open = false;

    @Override
    protected Collector<Long> collector() {
        return collector;
    }

    @Override
    protected Long getKey() {
        return key;
    }

    @Override
    public boolean shouldOpen() {
        return open;
    }

    public void setCollector(Collector<Long> collector) {
        this.collector = collector;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

}
