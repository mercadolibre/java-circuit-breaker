package com.mercadolibre.resilience.breaker.collector;

import com.mercadolibre.resilience.breaker.control.Consumer;
import com.mercadolibre.resilience.breaker.stats.Stats;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mercadolibre.resilience.breaker.TestUtil.*;

import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class BucketCollectorTest {

    private void assertState(Collector<?> collector, int measures, boolean rolling) throws NoSuchFieldException, IllegalAccessException {
        assertEquals(measures, ((ConcurrentMap<?,?>) getAttribute(collector, "measures")).size());
        assertEquals(measures, ((SortedSet<?>) getAttribute(collector, "timestamps")).size());
        assertEquals(rolling, ((AtomicBoolean) getAttribute(collector, "rolling")).get());
    }

    @Test
    public void shouldCollectAndRoll() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Stats>> sRef = new AtomicReference<>();

        Collector<Long> collector = new BucketCollector(2, 3, 1, new Consumer<List<Stats>>() {
            @Override
            public void consume(List<Stats> stats) {
                sRef.set(stats);
                latch.countDown();
            }
        });

        assertState(collector, 0, false);

        collector.collect(0L, true);

        assertState(collector, 1, false);

        collector.collect(1L, true);
        collector.collect(1L, true);
        collector.collect(2L, true);
        collector.collect(2L, false);
        collector.collect(2L, true);

        latch.await(1, TimeUnit.SECONDS);

        assertState(collector, 2, false);

        List<Stats> stats = sRef.get();

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1, stats.get(0).count());
        assertEquals(2, stats.get(1).count());
    }

    @Test
    public void shouldCollectAndRollWithStales() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Stats>> sRef = new AtomicReference<>();

        Collector<Long> collector = new BucketCollector(2, 1, 1, new Consumer<List<Stats>>() {
            @Override
            public void consume(List<Stats> stats) {
                sRef.set(stats);
                latch.countDown();
            }
        });

        assertState(collector, 0, false);

        collector.collect(0L, true);

        assertState(collector, 1, false);

        collector.collect(1L, true);
        collector.collect(1L, true);
        collector.collect(2L, true);
        collector.collect(2L, true);
        collector.collect(2L, true);

        latch.await(1, TimeUnit.SECONDS);

        assertState(collector, 2, false);

        List<Stats> stats = sRef.get();

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(2, stats.get(0).count());
    }

    @Test
    public void shouldCollectAndRollWithMinMetrics() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Stats>> sRef = new AtomicReference<>();

        Collector<Long> collector = new BucketCollector(2, 3, 2, new Consumer<List<Stats>>() {
            @Override
            public void consume(List<Stats> stats) {
                sRef.set(stats);
                latch.countDown();
            }
        });

        assertState(collector, 0, false);

        collector.collect(0L, true);

        assertState(collector, 1, false);

        collector.collect(1L, true);
        collector.collect(1L, true);
        collector.collect(2L, true);
        collector.collect(2L, true);
        collector.collect(2L, true);

        latch.await(1, TimeUnit.SECONDS);

        assertState(collector, 2, false);

        List<Stats> stats = sRef.get();

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(2, stats.get(0).count());
    }

}
