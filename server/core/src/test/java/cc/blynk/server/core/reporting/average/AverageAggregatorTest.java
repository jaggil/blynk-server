package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.ServerProperties;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import static cc.blynk.server.core.reporting.average.AverageAggregator.DAY;
import static cc.blynk.server.core.reporting.average.AverageAggregator.HOUR;
import static cc.blynk.utils.ReportingUtil.getReportingFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.08.15.
 */
public class AverageAggregatorTest {

    private final String reportingFolder = getReportingFolder(System.getProperty("java.io.tmpdir"));

    private static long getMillis(int year, int month, int dayOfMonth, int hour, int minute) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute);
        return dateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
    }

    @Test
    public void testAverageWorksOkForOnePin() {
        AverageAggregator averageAggregator = new AverageAggregator("");
        String username = "test";
        char pinType = PinType.VIRTUAL.pintTypeChar;
        int dashId = 1;
        byte pin = 1;

        long ts = getMillis(2015, 8, 1, 0, 0);

        int COUNT = 100;

        double expectedAverage = 0;
        for (int i = 0; i < COUNT; i++) {
            expectedAverage += i;
            averageAggregator.collect(username, dashId, 0, pinType, pin, ts, String.valueOf(i));
        }
        expectedAverage /= COUNT;

        assertEquals(1, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());

        assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(username, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        assertEquals(expectedAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, 0, pinType, pin, ts / DAY)).calcAverage(), 0);
    }

    @Test
    public void testAverageWorksForOneDay() {
        AverageAggregator averageAggregator = new AverageAggregator("");
        String username = "test";
        char pinType = PinType.VIRTUAL.pintTypeChar;
        int dashId = 1;
        byte pin = 1;

        double expectedDailyAverage = 0;

        int COUNT = 100;

        for (int hour = 0; hour < 24; hour++) {
            long ts = getMillis(2015, 8, 1, hour, 0);

            double expectedAverage = 0;
            for (int i = 0; i < COUNT; i++) {
                expectedAverage += i;
                averageAggregator.collect(username, dashId, 0, pinType, pin, ts, String.valueOf(i));
            }
            expectedDailyAverage += expectedAverage;
            expectedAverage /= COUNT;

            assertEquals(hour + 1, averageAggregator.getHourly().size());

            assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(username, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        }
        expectedDailyAverage /= COUNT * 24;

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, 0, pinType, pin, getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);
    }

    @Test
    public void testTempFilesCreated() throws IOException {
        Path dir = Paths.get(reportingFolder, "");
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        AverageAggregator averageAggregator = new AverageAggregator(reportingFolder);

        String username = "test";
        char pinType = PinType.VIRTUAL.pintTypeChar;
        int dashId = 1;
        byte pin = 1;

        double expectedDailyAverage = 0;

        int COUNT = 100;

        for (int hour = 0; hour < 24; hour++) {
            long ts = getMillis(2015, 8, 1, hour, 0);

            double expectedAverage = 0;
            for (int i = 0; i < COUNT; i++) {
                expectedAverage += i;
                averageAggregator.collect(username, dashId, 0, pinType, pin, ts, String.valueOf(i));
            }
            expectedDailyAverage += expectedAverage;
            expectedAverage /= COUNT;

            assertEquals(hour + 1, averageAggregator.getHourly().size());

            assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(username, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        }
        expectedDailyAverage /= COUNT * 24;

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, 0, pinType, pin, getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);


        averageAggregator.close();

        assertTrue(Files.exists(Paths.get(reportingFolder, AverageAggregator.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.exists(Paths.get(reportingFolder, AverageAggregator.DAILY_TEMP_FILENAME)));

        averageAggregator = new AverageAggregator(reportingFolder);

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, 0, pinType, pin, getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);

        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregator.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregator.DAILY_TEMP_FILENAME)));

        ReportingDao reportingDao = new ReportingDao(reportingFolder, null, new ServerProperties(Collections.emptyMap()));

        reportingDao.delete(username, dashId, 0, PinType.VIRTUAL, pin);
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregator.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregator.DAILY_TEMP_FILENAME)));
    }

}
