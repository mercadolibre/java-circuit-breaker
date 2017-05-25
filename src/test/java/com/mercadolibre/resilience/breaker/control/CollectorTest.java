package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.LockSupport;

public class CollectorTest extends ControlTestBase {

    private final Random random = new Random();

    private Stats loadStats(double errorRate, int size) {
        assert errorRate >= 0 && errorRate <= 1;

        Stats stats = new SimpleStats();
        for (int i = 0; i < size; i++) {
            if (random.nextDouble() < errorRate)
                stats.addFailure();
            else
                stats.addSuccess();
        }

        return stats;
    }

    @Test
    public void shouldCloseOnLowErrorRate() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.open();

        assertFalse(control.shouldClose());

        long now = control.getTimestamp();
        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - i, loadStats(0.1, 100));

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - i, loadStats(1, 100));

        collector.run();

        assertTrue(control.shouldClose());
    }

    @Test
    public void shouldOpenOnHighErrorRate() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.close();

        assertTrue(control.shouldClose());
        assertFalse(control.shouldOpen());

        long now = control.getTimestamp();
        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - i, loadStats(0.8, 100));

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - i, loadStats(0, 100));

        collector.run();

        assertFalse(control.shouldClose());
        assertTrue(control.shouldOpen());
    }

    @Test
    public void shouldBypassWithInsufficientSamples() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.close();

        assertTrue(control.shouldClose());

        long now = control.getTimestamp();
        stats.put(now, loadStats(0.8, 100));

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - i, loadStats(0, 100));

        collector.run();

        assertTrue(control.shouldClose());
    }

    @Test
    public void shouldRemoveIncompleteLastSample() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.open();

        assertFalse(control.shouldClose());

        long now = control.getTimestamp();

        stats.put(now, loadStats(0.1, 1));

        for (int i = 1; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - i, loadStats(0.1, 100));

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - i, loadStats(1, 100));

        collector.run();

        assertTrue(control.shouldClose());
    }

    @Test
    public void shouldDoNothingOnEmptyChunk() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.close();

        assertTrue(control.shouldClose());

        long now = control.getTimestamp();
        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - 1 - i, loadStats(1, 100));

        collector.run();

        assertTrue(control.shouldClose());
    }

    @Test
    public void shouldBypassWithIncompleteSample() {
        Collector collector = Collector.builder().build();
        OnOffCircuitControl control = OnOffCircuitControl.builder().withCollector(collector).startWorkers(false).build();

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        control.close();

        assertTrue(control.shouldClose());
        assertFalse(control.shouldOpen());

        long now = control.getTimestamp();
        stats.put(now, loadStats(0.8, 100));

        for (int i = 1; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - i, loadStats(0.1, 1));

        for (int i = 0; i < Collector.DEFAULT_WEIGHTS.length; i++)
            stats.put(now - Collector.DEFAULT_WEIGHTS.length - i, loadStats(1, 100));

        collector.run();

        assertTrue(control.shouldClose());
        assertFalse(control.shouldOpen());
    }

    @Test
    public void shouldStartWorkers() throws NoSuchFieldException, IllegalAccessException {
        OnOffCircuitControl control = OnOffCircuitControl.builder().build();

        ScheduledFuture<?> collectorFuture = (ScheduledFuture<?>) getAttribute("collectorFuture", control);
        ScheduledFuture<?> cleanerFuture = (ScheduledFuture<?>) getAttribute("cleanerFuture", control);

        assertNotNull(collectorFuture);
        assertNotNull(cleanerFuture);

        control.shutdown();

        LockSupport.parkNanos(3000000);

        assertTrue(collectorFuture.isCancelled());
        assertTrue(cleanerFuture.isCancelled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullWeights() {
        Collector.builder().withWeights(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyWeights() {
        Collector.builder().withWeights(new double[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTooLargeWeights() {
        Collector.builder().withWeights(new double[121]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectAllZeroWeights() {
        double[] weights = {0,0,0};
        Collector.builder().withWeights(weights);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeWeight() {
        double[] weights = {0.1,-0.4,0.4};
        Collector.builder().withWeights(weights);
    }

    @Test
    public void shouldBuildWithNormalizedWeights() {
        double[] weights = {0.1, 0.3, 0.6};
        Collector collector = Collector.builder().withWeights(weights).build();

        assertNotNull(collector);
        assertTrue(Arrays.equals(weights, collector.getWeights()));
    }

    @Test
    public void shouldBuildWithNonNormalizedWeights() {
        double[] weights = {1, 3, 6};
        Collector collector = Collector.builder().withWeights(weights).build();

        assertNotNull(collector);

        double[] normalized = {0.1, 0.3, 0.6};
        assertTrue(Arrays.equals(normalized, collector.getWeights()));
    }

}
