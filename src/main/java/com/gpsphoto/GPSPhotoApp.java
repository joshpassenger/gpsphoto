package com.gpsphoto;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.Silver;
import org.apache.log4j.Logger;

import javax.swing.*;

/**
 * Runs the photo matching user interface
 */
public class GPSPhotoApp
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhotoApp.class);

    public static void main(String[] args)
    {
        LOGGER.info("Setting look and feel");
        setLookAndFeel();

        LOGGER.info("Creating frame");
        GPSPhotoFrame frame = new GPSPhotoFrame();
        frame.setLocationRelativeTo(null);

        LOGGER.info("Showing frame");
        frame.setVisible(true);
        LOGGER.info("Startup complete");

        frame.addLog("INFO", String.format("GPS Photo Manager (%s) was started successfully", frame.VERSION));
    }

    /**
     * Sets the look and feel for the application
     */
    private static void setLookAndFeel()
    {
        PlasticXPLookAndFeel.setPlasticTheme(new Silver());
        try
        {
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        }
        catch (Exception e)
        {
        }
    }
}
