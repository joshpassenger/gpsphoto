package com.gpsphoto;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

/**
 * Tests photo matching
 */
public class GPSPhotoTest
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhotoTest.class);

    @Test
    public void testPhotoMatching() throws GPSPhotoException
    {
        List<GPSPosition> positions = new ArrayList<>();

        Date now = new Date();
        Date nowUTC = DateUtils.addHours(now, -10);

        double latitude = -25.0;
        double longitude = 120.0;

        for (int i = 0; i < 10; ++i)
        {
            GPSPosition position = new GPSPosition(latitude, longitude, nowUTC);
            positions.add(position);
            latitude += 0.000001;
            longitude += 0.000001;
            nowUTC = DateUtils.addSeconds(nowUTC, 2);
        }

        GPSPhoto photo = new GPSPhoto(new File("some.jpg"));
        photo.setCaptureTime(DateUtils.addSeconds(now, 5));
        photo.applyTimeDelta(-1000L);
        assertTrue(photo.computePosition(positions, 2000L));

        photo = new GPSPhoto(new File("some.jpg"));
        photo.setCaptureTime(DateUtils.addSeconds(now, -5));
        photo.applyTimeDelta(-1000L);
        assertFalse(photo.computePosition(positions, 2000L));

        photo = new GPSPhoto(new File("some.jpg"));
        photo.setCaptureTime(DateUtils.addSeconds(now, 50));
        photo.applyTimeDelta(1000L);
        assertFalse(photo.computePosition(positions, 2000L));

        LOGGER.info("Tests complete");

    }
}
