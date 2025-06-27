package hudson.plugins.performance.reports;

import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.reports.UriReport.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UriReportTest {

    private static final String HTTP_200 = "200";
    private static final long AVERAGE = 5;
    private static final long MIN = 0;
    private static final long MAX = 10;
    private UriReport uriReport;

    @BeforeEach
    void setUp() {
        PerformanceReport performanceReport = new PerformanceReport(PerformanceReportTest.DEFAULT_PERCENTILES);
        uriReport = new UriReport(performanceReport, null, null);
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDuration(MAX);
        Date date = new Date();
        httpSample1.setDate(date);
        httpSample1.setSuccessful(false);
        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDuration(AVERAGE);
        httpSample2.setDate(date);
        httpSample2.setSuccessful(true);
        HttpSample httpSample3 = new HttpSample();
        httpSample3.setDuration(MIN);
        httpSample3.setDate(date);
        httpSample3.setSuccessful(false);
        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);
        uriReport.addHttpSample(httpSample3);
    }

    @Test
    void testHasSamples() throws Exception {
        assertTrue(uriReport.hasSamples());
    }

    @Test
    void testCountErrors() {
        assertEquals(2, uriReport.countErrors());
    }

    @Test
    void testGetAverage() {
        assertEquals(AVERAGE, uriReport.getAverage());
    }

    @Test
    void testGetMedian() {
        // For 3 samples sorted as [0, 5, 10], the 50th percentile is calculated as index `(int)(3*0.5)-1=0`, which is the first element.
        assertEquals(MIN, uriReport.getMedian());
    }

    @Test
    void testGet90Line() {
        // For 3 samples sorted as [0, 5, 10], the 90th percentile is calculated as index `(int)(3*0.9)-1=1`, which is the second element.
        assertEquals(AVERAGE, uriReport.get90Line());
    }

    @Test
    void testGet95Line() {
        // For 3 samples sorted as [0, 5, 10], the 95th percentile is calculated as index `(int)(3*0.95)-1=1`, which is the second element.
        assertEquals(AVERAGE, uriReport.get95Line());
    }

    @Test
    void testErrorPercent() {
        assertEquals(66.667, uriReport.errorPercent(), 0.001);
    }

    @Test
    void testSamplesCount() {
        assertEquals(3, uriReport.samplesCount());
    }

    @Test
    void testGetHttpSampleList() {
        assertEquals(3, uriReport.getHttpSampleList().size());
    }

    @Test
    void testGetMax() {
		assertEquals(MAX, uriReport.getMax());
	}

    @Test
    void testGetMin() {
		assertEquals(MIN, uriReport.getMin());
	}

    @Test
    void testIsFailed() {
        assertTrue(uriReport.isFailed());
    }

    @Test
    void testEmptyReport() {
        UriReport emptyReport = new UriReport(new PerformanceReport(), "empty", "empty");
        assertTrue(emptyReport.getHttpSampleList().isEmpty());
        assertFalse(emptyReport.hasSamples());
        assertEquals(0, emptyReport.countErrors());
        assertEquals(0, emptyReport.getAverage());
        assertEquals(0, emptyReport.getMax());
        assertEquals(0, emptyReport.getMin());
        assertEquals(0, emptyReport.getMedian());
        assertFalse(emptyReport.isFailed());
        assertEquals(0, emptyReport.errorPercent(), 0.0);
        assertEquals(0, emptyReport.samplesCount());
    }

    @Test
    void testDiffs() {
        UriReport lastReport = new UriReport(new PerformanceReport(), "test", "test");
        HttpSample s1 = new HttpSample();
        s1.setDuration(4);
        s1.setSuccessful(true);
        lastReport.addHttpSample(s1);
        HttpSample s2 = new HttpSample();
        s2.setDuration(8);
        s2.setSuccessful(true);
        lastReport.addHttpSample(s2);

        uriReport.addLastBuildUriReport(lastReport);

        // current: avg=5, median=0, error%=66.667, samples=3
        // last: avg=(4+8)/2=6, median=4 (1st of [4,8]), error%=0, samples=2
        assertEquals(-1, uriReport.getAverageDiff());
        assertEquals(-4, uriReport.getMedianDiff());
        assertEquals(66.667, uriReport.getErrorPercentDiff(), 0.001);
        assertEquals(1, uriReport.getSamplesCountDiff());
    }

    /**
     * Same dates, different duration. Shortest duration should be ordered first.
     */
    @Test
    void testCompareSameDateDifferentDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(1), 2, HTTP_200, true, false));
        samples.add(new Sample(new Date(1), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().duration);
        assertEquals(2, iter.next().duration);
    }

    /**
     * Different dates, same duration. Oldest date should be ordered first.
     */
    @Test
    void testCompareDifferentDateSameDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(2), 1, HTTP_200, true, false));
        samples.add(new Sample(new Date(1), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().date.getTime());
        assertEquals(2, iter.next().date.getTime());
    }

    /**
     * Different dates, different duration. Shortest duration should be ordered first.
     */
    @Test
    void testCompareDifferentDateDifferentDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(1), 2, HTTP_200, true, false));
        samples.add(new Sample(new Date(2), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().duration);
        assertEquals(2, iter.next().duration);
    }

    /**
     * Null dates. Ordering is unspecified, but should not cause exceptions.
     */
    @Test
    void testCompareNullDateSameDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(null, 1, HTTP_200, true, false));
        samples.add(new Sample(null, 1, HTTP_200, true, false));

        assertDoesNotThrow(() -> {
            // execute system under test
            Collections.sort(samples);
        }, "A NullPointerException was thrown (which should not have happened).");
    }
}
