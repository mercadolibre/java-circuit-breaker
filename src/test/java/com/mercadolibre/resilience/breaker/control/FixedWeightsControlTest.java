package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.SimpleStats;
import com.mercadolibre.resilience.breaker.stats.Stats;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static com.mercadolibre.resilience.breaker.TestUtil.*;


public class FixedWeightsControlTest {

    @Test
    public void shouldCalculateScore() throws Exception {
        FixedWeightsControl control = new FixedWeightsControl(new double[]{5,3,2}, 0.5, 5, 100, 10);

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

        assertEquals(0.680, score, 0.001);
    }

}
