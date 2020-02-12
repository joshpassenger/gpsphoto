package com.gpsphoto;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Processes a single photo
 */
public class GPSPhotoThread implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhotoThread.class);

    private final List<GPSPosition> positions;
    private final long tolerance;

    private final LinkedBlockingQueue<GPSPhoto> photos;
    private final LinkedBlockingQueue<GPSPhoto> complete;
    private final LinkedBlockingQueue<GPSPhoto> failed;

    public GPSPhotoThread(List<GPSPosition> positions,
                          long tolerance,
                          LinkedBlockingQueue<GPSPhoto> photos,
                          LinkedBlockingQueue<GPSPhoto> complete,
                          LinkedBlockingQueue<GPSPhoto> failed)
    {
        this.positions = positions;
        this.tolerance = tolerance;
        this.photos = photos;
        this.complete = complete;
        this.failed = failed;
    }

    public void run()
    {
        while (!photos.isEmpty())
        {
            GPSPhoto photo = null;
            try
            {
                photo = photos.poll(100L, TimeUnit.MILLISECONDS);

                if (photo != null)
                {
                    if (photo.computePosition(positions, tolerance))
                    {
                        photo.setMatched(true);
                        photo.computeThumbnail();
                        complete.add(photo);
                    }
                    else
                    {
                        failed.add(photo);
                    }
                }
            }
            catch (InterruptedException e)
            {
                LOGGER.error("Got interrupted processing image", e);
            }
            catch (Throwable t)
            {
                LOGGER.error("Failed to process image", t);

                if (photo != null)
                {
                    failed.add(photo);
                }
            }
        }


    }
}
