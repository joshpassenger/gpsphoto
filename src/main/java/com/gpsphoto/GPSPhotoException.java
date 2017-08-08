package com.gpsphoto;

public class GPSPhotoException extends Exception
{
    public GPSPhotoException(String message)
    {
        super(message);
    }

    public GPSPhotoException(String message, Throwable t)
    {
        super(message, t);
    }
}