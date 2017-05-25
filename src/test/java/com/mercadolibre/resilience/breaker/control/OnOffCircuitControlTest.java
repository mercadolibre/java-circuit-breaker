package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.Action;
import com.mercadolibre.resilience.breaker.stats.Stats;
import com.mercadolibre.resilience.breaker.util.TestUtil;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ScheduledFuture;

public class OnOffCircuitControlTest extends ControlTestBase {

    @Test
    public void shouldRegisterSuccess() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        Action<Boolean> action = new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return result && e == null;
            }
        };

        control.register(action, true);

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        assertEquals(1, stats.size());

        Stats s = stats.entrySet().iterator().next().getValue();

        assertEquals(1, s.successCount());
        assertEquals(0, s.failureCount());
    }

    @Test
    public void shouldRegisterFailure() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        Action<Boolean> action = new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return false;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return result && e == null;
            }
        };

        control.register(action, false);

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        assertEquals(1, stats.size());

        Stats s = stats.entrySet().iterator().next().getValue();

        assertEquals(0, s.successCount());
        assertEquals(1, s.failureCount());
    }

    @Test
    public void shouldRegisterException() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        Action<Boolean> action = new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return Boolean.TRUE.equals(result) && e == null;
            }
        };

        control.register(action, new RuntimeException());

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        assertEquals(1, stats.size());

        Stats s = stats.entrySet().iterator().next().getValue();

        assertEquals(0, s.successCount());
        assertEquals(1, s.failureCount());
    }

    @Test
    public void shouldGroupRegisteredData() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        Action<Boolean> action = new Action<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                return true;
            }

            @Override
            public boolean isValid(Boolean result, Exception e) {
                return result && e == null;
            }
        };

        for (int i = 0; i < 1000; i++)
            control.register(action, true);

        ConcurrentNavigableMap<Long,Stats> stats = getStats(control);

        assertFalse(stats.isEmpty());

        long size = 0;
        for (Stats s : stats.values()) {
            if (s.count() > size)
                size = s.count();
        }

        assertTrue(size > 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeMinScore() {
        OnOffCircuitControl.builder().withMinScore(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeMinSampleSize() {
        OnOffCircuitControl.builder().withMinSampleSize(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullCollector() {
        OnOffCircuitControl.builder().withCollector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeCollectorThreadDelay() {
        OnOffCircuitControl.builder().withCollectorThreadDelay(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveCollectorThreadInterval() {
        OnOffCircuitControl.builder().withCollectorThreadInterval(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeCleanerThreadDelay() {
        OnOffCircuitControl.builder().withCleanerThreadDelay(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveCleanerThreadInterval() {
        OnOffCircuitControl.builder().withCleanerThreadInterval(0);
    }

    @Test
    public void shouldBuild() {
        Collector collector = Collector.builder().build();

        OnOffCircuitControl control = OnOffCircuitControl.builder()
                .withMinScore(OnOffCircuitControl.DEFAULT_MIN_SCORE+1)
                .withMinSampleSize(OnOffCircuitControl.DEFAULT_MIN_SAMPLE_SIZE+1)
                .withCollector(collector)
                .startWorkers(false)
                .withCollectorThreadDelay(OnOffCircuitControl.DEFAULT_COLLECTOR_THREAD_DELAY+1)
                .withCollectorThreadInterval(OnOffCircuitControl.DEFAULT_COLLECTOR_THREAD_INTERVAL+1)
                .withCleanerThreadDelay(OnOffCircuitControl.DEFAULT_CLEANER_THREAD_DELAY+1)
                .withCleanerThreadInterval(OnOffCircuitControl.DEFAULT_CLEANER_THREAD_INTERVAL+1)
                .build();

        assertNotNull(control);
        assertEquals(OnOffCircuitControl.DEFAULT_MIN_SCORE+1, control.getMinScore(), 0.01);
        assertEquals(OnOffCircuitControl.DEFAULT_MIN_SAMPLE_SIZE+1, control.getMinSampleSize(), 0.01);
        assertNull(TestUtil.getAttribute("collectorFuture", control));
        assertNull(TestUtil.getAttribute("cleanerFuture", control));
        assertTrue(TestUtil.getAttribute("collector", control) == collector);
    }

    @Test
    public void shouldShutdownStarted() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(true).build();

        assertNotNull(control);

        ScheduledFuture<?> collectorFuture = (ScheduledFuture<?>) TestUtil.getAttribute("collectorFuture", control);
        ScheduledFuture<?> cleanerFuture = (ScheduledFuture<?>) TestUtil.getAttribute("cleanerFuture", control);

        assertNotNull(collectorFuture);
        assertNotNull(cleanerFuture);

        control.shutdown();

        assertTrue(collectorFuture.isCancelled());
        assertTrue(cleanerFuture.isCancelled());
    }

    @Test
    public void shouldShutdownNonStarted() {
        OnOffCircuitControl control = OnOffCircuitControl.builder().startWorkers(false).build();

        assertNotNull(control);

        ScheduledFuture<?> collectorFuture = (ScheduledFuture<?>) TestUtil.getAttribute("collectorFuture", control);
        ScheduledFuture<?> cleanerFuture = (ScheduledFuture<?>) TestUtil.getAttribute("cleanerFuture", control);

        assertNull(collectorFuture);
        assertNull(cleanerFuture);

        control.shutdown();

        assertNull(collectorFuture);
        assertNull(cleanerFuture);
    }

    @Test
    public void shouldStartOnlyOnce() {
        OnOffCircuitControl.Builder builder = OnOffCircuitControl.builder().startWorkers(true);
        OnOffCircuitControl control = builder.build();

        assertNotNull(control);

        ScheduledFuture<?> collectorFuture = (ScheduledFuture<?>) TestUtil.getAttribute("collectorFuture", control);
        ScheduledFuture<?> cleanerFuture = (ScheduledFuture<?>) TestUtil.getAttribute("cleanerFuture", control);

        assertNotNull(collectorFuture);
        assertNotNull(cleanerFuture);

        assertTrue(control == builder.build());
        assertTrue(collectorFuture == TestUtil.getAttribute("collectorFuture", control));
        assertTrue(cleanerFuture == TestUtil.getAttribute("cleanerFuture", control));
    }

}
