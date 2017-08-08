package com.gpsphoto;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * A loaded raw photo for capture time
 */
public class GPSPhoto
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhoto.class);

    private final File inputFile;

    private Double latitude;
    private Double longitude;

    private Date captureTime;
    private Date offsetTime;

    private Metadata metadata;

    private int thumbnailWidth = 512;
    private int thumbnailHeight = 512;
    private double thumbnailQuality = 0.9;

    private File thumbnailFile;

    boolean matched = false;

    public GPSPhoto(File inputFile)
    {
        this.inputFile = inputFile;
    }

    public Date getCaptureTime()
    {
        return captureTime;
    }

    public Date getOffsetTime()
    {
        return offsetTime;
    }

    public File getInputFile()
    {
        return inputFile;
    }

    public Double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(Double latitude)
    {
        this.latitude = latitude;
    }

    public Double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(Double longitude)
    {
        this.longitude = longitude;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public void setThumbnailFile(File thumbnailFile)
    {
        this.thumbnailFile = thumbnailFile;
    }

    public void computeThumbnail() throws IOException
    {
        if (thumbnailFile == null)
        {
            return;
        }

        Thumbnails.of(inputFile)
                .size(thumbnailWidth, thumbnailHeight)
                .outputQuality(thumbnailQuality)
                .toFile(thumbnailFile);
    }

    public void loadMetaData() throws GPSPhotoException
    {
        try
        {
            this.metadata = ImageMetadataReader.readMetadata(inputFile);

            Collection<ExifIFD0Directory> exifIFD0Directory = metadata.getDirectoriesOfType(ExifIFD0Directory.class);

            for (ExifIFD0Directory directory: exifIFD0Directory)
            {
                captureTime = directory.getDate(ExifIFD0Directory.TAG_DATETIME, TimeZone.getDefault());
            }
        }
        catch (Throwable t)
        {
            throw new GPSPhotoException("Failed to extract image meta data from: " + inputFile, t);
        }

        if (captureTime == null)
        {
            throw new GPSPhotoException("Input image had no capture time in meta data: " + inputFile);
        }
    }

    public void applyTimeDelta(long timeOffset)
    {
        offsetTime = new Date(captureTime.getTime() + timeOffset);
    }

    /**
     * Hunt for a position where the offset time is equal or greater to
     * to the time in the positions
     */
    public boolean computePosition(List<GPSPosition> positions, long tolerance) throws GPSPhotoException
    {
        long offsetTimeMillis = offsetTime.getTime();

        for (int i = 0; i < positions.size() - 1; ++i)
        {
            GPSPosition thisPosition = positions.get(i);
            GPSPosition nextPosition = positions.get(i + 1);

            long thisTime = thisPosition.getLocalTime().getTime();
            long nextTime = nextPosition.getLocalTime().getTime();

            if (thisTime == nextTime)
            {
                continue;
            }

            if (thisTime > nextTime)
            {
                throw new GPSPhotoException("GPS positions are not sorted by increasing time");
            }

            boolean greater = thisTime <= offsetTimeMillis;
            boolean less = offsetTimeMillis <= nextTime;
            long timeDelta1 = offsetTimeMillis - thisTime;
            long timeDelta2 = nextTime - offsetTimeMillis;

            boolean timeMatch1 = timeDelta1 >= 0L && timeDelta1 <= tolerance;
            boolean timeMatch2 = timeDelta2 >= 0L && timeDelta2 <= tolerance;

            /**
             * Hunt linearly for a matching time slot
             */
            if (greater && less && timeMatch1 && timeMatch2)
            {
                /**
                 * Linearly interpolate the position
                 */
                double timeDelta = nextTime - thisTime;
                double timeOffset = offsetTimeMillis - thisTime;
                double ratio = timeOffset / timeDelta;

                double newLatitude = (1.0 - ratio) * thisPosition.getLatitude() + ratio * nextPosition.getLatitude();
                double newLongitude = (1.0 - ratio) * thisPosition.getLongitude() + ratio * nextPosition.getLongitude();

                setLatitude(newLatitude);
                setLongitude(newLongitude);

                return true;
            }
        }

        LOGGER.info("Did not find a match for photo: " + inputFile.getName());
        return false;
    }

    public String getFormattedCaptureTime()
    {
        return FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(captureTime);
    }

    public boolean isMatched()
    {
        return matched;
    }

    public void setMatched(boolean matched)
    {
        this.matched = matched;
    }

    public void setCaptureTime(Date captureTime)
    {
        this.captureTime = captureTime;
    }
}
