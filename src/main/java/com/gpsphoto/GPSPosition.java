package com.gpsphoto;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;
import java.util.TimeZone;

/**
 * GPS position that stores positions in WGS84 with
 * associated capture time in UTC (with leap seconds subtracted) and local time.
 */
public class GPSPosition implements Comparable<GPSPosition>
{
    private final double latitude;
    private final double longitude;
    private final Date utcTime;
    private final Date localTime;

    public GPSPosition(double latitude, double longitude, Date utcTime)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.utcTime = utcTime;
        int offsetToLocalTime = TimeZone.getDefault().getOffset(utcTime.getTime());
        this.localTime = DateUtils.addMilliseconds(utcTime, offsetToLocalTime);
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public Date getUTCTime()
    {
        return utcTime;
    }

    public Date getLocalTime()
    {
        return localTime;
    }

    @Override
    public int compareTo(GPSPosition o)
    {
        return utcTime.compareTo(o.utcTime);
    }

    public String toString()
    {
        FastDateFormat fdf = FastDateFormat.getInstance("dd/MM/yyyy HH:mm:ss");
        return String.format("Local time: [%s] UTC time: [%s] Lat: [%.16f] Lon: [%.16f]",
                             fdf.format(localTime),
                             fdf.format(utcTime),
                             latitude,
                             longitude);
    }
}
