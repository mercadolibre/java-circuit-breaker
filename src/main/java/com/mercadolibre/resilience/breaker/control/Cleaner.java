package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.Stats;

import java.util.concurrent.ConcurrentNavigableMap;

class Cleaner implements Runnable {

    protected static final int CLEANER_GAP = 2;

    private final OnOffCircuitControl control;

    public Cleaner(OnOffCircuitControl control) {
        this.control = control;
    }

    private Long fromDate() {
        return control.getTimestamp() - control.getCollector().getWeights().length - CLEANER_GAP;
    }

    public void run() {
        ConcurrentNavigableMap<Long,Stats> chunk = control.getStats().headMap(fromDate(), true);
        for (Long key : chunk.keySet())
            control.getStats().remove(key);
    }

}
