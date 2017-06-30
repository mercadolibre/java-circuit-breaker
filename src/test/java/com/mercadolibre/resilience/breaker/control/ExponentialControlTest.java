package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static com.mercadolibre.resilience.breaker.TestUtil.*;
import static org.junit.Assert.*;


public class ExponentialControlTest {

    @Test
    public void shouldCalculateScore() throws Exception {
        ExponentialControl control = new ExponentialControl(0.2, 3, 0.5, 5, 100, 10);

        Stats s0 = new SimpleStats();
        setAttribute(s0, "successes", new AtomicLong(8));
        setAttribute(s0, "failures", new AtomicLong(2));

        Stats s1 = new SimpleStats();
        setAttribute(s1, "successes", new AtomicLong(6));
        setAttribute(s1, "failures", new AtomicLong(4));

        Stats s2 = new SimpleStats();
        setAttribute(s2, "successes", new AtomicLong(5));
        setAttribute(s2, "failures", new AtomicLong(5));

        double score = control.score(Arrays.asList(s0, s1, s2));

        assertEquals(0.7520, score, 0.0001);
    }

    @Test
    public void shouldGetKey() throws Exception {
        ExponentialControl control = new ExponentialControl(0.2, 3, 0.5, 5, 100, 10);

        invoke(control, "doSwitch", 0.3);

        assertTrue(control.shouldOpen());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNonPositiveBuckets() {
        new ExponentialControl(0.2, 0, 0.5, 5, 100, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeScore() {
        new ExponentialControl(0.2, 2, -1, 1, 100, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowLargeScore() {
        new ExponentialControl(0.2, 2, 1.1, 1, 100, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeStaleInterval() {
        new ExponentialControl(0.2, 2, 0.5, -1, 100, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNonPositiveBucketWidth() {
        new ExponentialControl(0.2, 1, 0.5, 5, 0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNegativeMinMeasures() {
        new ExponentialControl(0.2, 1, 0.5, 5, 100, -1);
    }

}
