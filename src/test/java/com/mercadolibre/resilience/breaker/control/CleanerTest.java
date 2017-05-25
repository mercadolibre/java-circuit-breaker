package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentNavigableMap;

public class CleanerTest extends ControlTestBase {

    @Test
    public void shouldCleanOldData() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        long now = control.getTimestamp();

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length + Cleaner.CLEANER_GAP + 5; i++)
            stats.put(now - i, new SimpleStats());

        Cleaner cleaner = new Cleaner(control);

        cleaner.run();

        assertEquals(Collector.DEFAULT_WEIGHTS.length + Cleaner.CLEANER_GAP, stats.size());
    }


}
