package com.gpsphoto;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Main frame for GPS Photo user interface
 */
public class GPSPhotoFrame extends JFrame
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhotoFrame.class);

    private JTabbedPane tabbedPane = new JTabbedPane();

    private JPanel configPanel = new JPanel(new BorderLayout());
    private GridBagLayout configGridBag = new GridBagLayout();
    private JPanel configTopPanel = new JPanel(configGridBag);
    private GridBagConstraints c = new GridBagConstraints();

    private JPanel configButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private JButton processButton = new JButton("Process images");
    private JButton exitButton = new JButton("   Exit   ");
    private JButton clearLogsButton = new JButton("Clear logs");

    private JPanel logsPanel = new JPanel(new BorderLayout());
    private JPanel logsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private JTextArea logTextArea = new JTextArea();

    private JTextField photosDirTextField = new JTextField();
    private JTextField timePhotoTextField = new JTextField();
    private JTextField timeTextField = new JTextField();
    private JTextField gpxFileTextField = new JTextField();
    private JTextField fileNameTextField = new JTextField();
    private JTextField outputDirTextField = new JTextField();
    private JTextField timeToleranceTextField = new JTextField();
    private JTextField coresTextField = new JTextField();
    private JCheckBox recursivePhotosCheckBox = new JCheckBox("Recursive search");
    private JCheckBox exportShapeCheckBox = new JCheckBox("Export shape file");
    private JCheckBox exportKMLCheckBox = new JCheckBox("Export kml file");
    private JCheckBox thumbnailsCheckBox = new JCheckBox("Export thumbnails");
    private JCheckBox saveGPXTrackCheckBox = new JCheckBox("Export raw gpx file");

    private JButton browsePhotosButton = new JButton("Browse");
    private JButton browseTimePhotoButton = new JButton("Browse");
    private JButton viewTimePhotoButton = new JButton("View");
    private JButton browseGPXFileButton = new JButton("Browse");
    private JButton outputFolderButton = new JButton("Browse");
    private JButton readCaptureTimeButton = new JButton("Load time");

    private JProgressBar progressBar = new JProgressBar();

    public static String VERSION = "1.0.7 (validation trial)";

    private FastDateFormat logDateFormat = FastDateFormat.getInstance("dd-MM-yyyy HH:mm:ss");

    protected final GPSPhotoFrame baseFrame;

    public GPSPhotoFrame()
    {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        baseFrame = this;
        init();
    }

    protected void createStrut(int height)
    {
        Component verticalStrut = Box.createVerticalStrut(height);
        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        configGridBag.setConstraints(verticalStrut, c);
        configTopPanel.add(verticalStrut);
    }

    private void createLabel(String text)
    {
        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JLabel label = new JLabel(text);
        configGridBag.setConstraints(label, c);
        configTopPanel.add(label);
    }

    private void init()
    {
        setSize(new Dimension(800, 650));
        setMinimumSize(new Dimension(800, 650));
        setTitle("GPS Photo Manager: " + VERSION);
        setResizable(true);

        getRootPane().setLayout(new BorderLayout());

        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        /**
         * Config panel
         */
        configPanel.setBorder(BorderFactory.createEmptyBorder());
        configButtonPanel.setBorder(BorderFactory.createEmptyBorder());
        configTopPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        configButtonPanel.add(exitButton);
        configButtonPanel.add(processButton);

        configPanel.add(configButtonPanel, BorderLayout.SOUTH);
        configPanel.add(configTopPanel, BorderLayout.CENTER);
        tabbedPane.add("Configuration", configPanel);

        /**
         * Populate config panel
         */
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 2, 0, 2);

        createLabel("Output file name prefix:");

        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        FastDateFormat fdf = FastDateFormat.getInstance("yyyyMMdd_");
        fileNameTextField.setText(fdf.format(new Date(System.currentTimeMillis())));
        fileNameTextField.setMinimumSize(new Dimension(100, 30));
        configGridBag.setConstraints(fileNameTextField, c);
        configTopPanel.add(fileNameTextField);

        createStrut(10);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JLabel label = new JLabel("Input photo directory:");
        configGridBag.setConstraints(label, c);
        configTopPanel.add(label);

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        configGridBag.setConstraints(photosDirTextField, c);
        configTopPanel.add(photosDirTextField);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        configGridBag.setConstraints(browsePhotosButton, c);
        configTopPanel.add(browsePhotosButton);

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        Component glue = Box.createGlue();
        configGridBag.setConstraints(glue, c);
        configTopPanel.add(glue);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        recursivePhotosCheckBox.setSelected(true);
        recursivePhotosCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
        configGridBag.setConstraints(recursivePhotosCheckBox, c);
        configTopPanel.add(recursivePhotosCheckBox);

        createLabel("Photo of GPS showing local time:");

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        configGridBag.setConstraints(timePhotoTextField, c);
        configTopPanel.add(timePhotoTextField);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        configGridBag.setConstraints(browseTimePhotoButton, c);
        configTopPanel.add(browseTimePhotoButton);

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        glue = Box.createGlue();
        configGridBag.setConstraints(glue, c);
        configTopPanel.add(glue);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        viewTimePhotoButton.setEnabled(false);
        configGridBag.setConstraints(viewTimePhotoButton, c);
        configTopPanel.add(viewTimePhotoButton);

        createLabel("Time shown on GPS photo (yyyy-MM-dd HH:mm:ss)");

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        timeTextField.setMinimumSize(new Dimension(100, 30));
        configGridBag.setConstraints(timeTextField, c);
        configTopPanel.add(timeTextField);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        readCaptureTimeButton.setEnabled(false);
        configGridBag.setConstraints(readCaptureTimeButton, c);
        configTopPanel.add(readCaptureTimeButton);

        createStrut(10);

        createLabel("GPX file:");

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        configGridBag.setConstraints(gpxFileTextField, c);
        configTopPanel.add(gpxFileTextField);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        configGridBag.setConstraints(browseGPXFileButton, c);
        configTopPanel.add(browseGPXFileButton);

        createStrut(10);

        createLabel("Output directory:");

        c.weightx = 6.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        configGridBag.setConstraints(outputDirTextField, c);
        configTopPanel.add(outputDirTextField);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        configGridBag.setConstraints(outputFolderButton, c);
        configTopPanel.add(outputFolderButton);

        createStrut(10);

        createLabel("GPS time tolerance (milliseconds):");

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        timeToleranceTextField.setText("2000");
        timeToleranceTextField.setMinimumSize(new Dimension(100, 30));
        configGridBag.setConstraints(timeToleranceTextField, c);
        configTopPanel.add(timeToleranceTextField);

        createStrut(10);

        createLabel("Number of CPU cores to use for processing:");

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        coresTextField.setText("" + Runtime.getRuntime().availableProcessors());
        coresTextField.setMinimumSize(new Dimension(100, 30));
        configGridBag.setConstraints(coresTextField, c);
        configTopPanel.add(coresTextField);

        createStrut(10);

        createLabel("Export options:");

        createStrut(5);

        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        saveGPXTrackCheckBox.setSelected(true);
        configGridBag.setConstraints(saveGPXTrackCheckBox, c);
        configTopPanel.add(saveGPXTrackCheckBox);

        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        exportShapeCheckBox.setSelected(true);
        configGridBag.setConstraints(exportShapeCheckBox, c);
        configTopPanel.add(exportShapeCheckBox);

        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        thumbnailsCheckBox.setSelected(true);
        configGridBag.setConstraints(thumbnailsCheckBox, c);
        configTopPanel.add(thumbnailsCheckBox);

        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        exportKMLCheckBox.setSelected(true);
        configGridBag.setConstraints(exportKMLCheckBox, c);
        configTopPanel.add(exportKMLCheckBox);

        createStrut(10);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        configGridBag.setConstraints(progressBar, c);
        configTopPanel.add(progressBar);

        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        glue = Box.createGlue();
        configGridBag.setConstraints(glue, c);
        configTopPanel.add(glue);

        /**
         * Log panel
         */
        logsPanel.setBorder(BorderFactory.createEmptyBorder());
        logsButtonPanel.add(clearLogsButton);
        JScrollPane logScroll = new JScrollPane();

        logScroll.getViewport().add(logTextArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logsPanel.add(logScroll, BorderLayout.CENTER);
        logsPanel.add(logsButtonPanel, BorderLayout.SOUTH);
        tabbedPane.add("Logs", logsPanel);

        getRootPane().add(tabbedPane, BorderLayout.CENTER);

        registerListeners();
    }

    private void exit()
    {
        addLog("INFO", "GPS Photo Manager exiting normally");
        System.exit(0);
    }

    private void registerListeners()
    {
        clearLogsButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                logTextArea.setText("");
                addLog("INFO", "Logs cleared");
            }
        });

        exitButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Exiting application vua exit button");
                exit();
            }
        });

        readCaptureTimeButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Reading capture time from image");

                try
                {
                    GPSPhoto photo = new GPSPhoto(new File(timePhotoTextField.getText()));
                    photo.loadMetaData();
                    timeTextField.setText(photo.getFormattedCaptureTime());
                }
                catch (GPSPhotoException e1)
                {
                    timeTextField.setText("Failed to read time, check logs");
                    addLog("ERROR", "Failed to read capture time from image: " + e1.toString());
                }

            }
        });


        browsePhotosButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Browsing for photo directory");

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setMultiSelectionEnabled(false);

                if (StringUtils.isNotBlank(photosDirTextField.getText()))
                {
                    chooser.setCurrentDirectory(new File(photosDirTextField.getText()).getParentFile());
                }

                int result = chooser.showDialog(baseFrame, "Select directory");

                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File inputDirectory = chooser.getSelectedFile();
                    addLog("INFO", "User selected input directory: " + inputDirectory.toString());
                    photosDirTextField.setText(inputDirectory.toString());
                }
            }
        });

        outputFolderButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Browsing for output folder");

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setMultiSelectionEnabled(false);

                if (StringUtils.isNotBlank(outputDirTextField.getText()))
                {
                    chooser.setCurrentDirectory(new File(outputDirTextField.getText()).getParentFile());
                }
                else if (StringUtils.isNotBlank(photosDirTextField.getText()))
                {
                    addLog("INFO", "Setting output start folder to parent of: " + photosDirTextField.getText());
                    chooser.setCurrentDirectory(new File(photosDirTextField.getText()).getParentFile());
                }

                int result = chooser.showDialog(baseFrame, "Select directory");

                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File outputDirectory = chooser.getSelectedFile();
                    addLog("INFO", "User selected output directory: " + outputDirectory.toString());
                    outputDirTextField.setText(outputDirectory.toString());
                }
            }
        });

        browseTimePhotoButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Browsing for time photo");

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File f)
                    {
                        return FilenameUtils.isExtension(f.getName().toLowerCase(), new String [] {"jpg", "jpeg" });
                    }

                    @Override
                    public String getDescription()
                    {
                        return "Image files";
                    }
                });
                chooser.setMultiSelectionEnabled(false);

                if (StringUtils.isNotBlank(photosDirTextField.getText()))
                {
                    addLog("INFO", "Setting time photo start folder: " + photosDirTextField.getText());
                    chooser.setCurrentDirectory(new File(photosDirTextField.getText()));
                }

                int result = chooser.showDialog(baseFrame, "Select photo");

                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File timePhoto = chooser.getSelectedFile();
                    addLog("INFO", "User selected time photo: " + timePhoto.toString());
                    timePhotoTextField.setText(timePhoto.toString());
                }
            }
        });

        browseGPXFileButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Browsing for GPX file ");

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File f)
                    {
                        return f.isDirectory() || FilenameUtils.isExtension(f.getName().toLowerCase(), new String [] {"gpx"});
                    }

                    @Override
                    public String getDescription()
                    {
                        return "GPX files";
                    }
                });
                chooser.setMultiSelectionEnabled(false);

                if (StringUtils.isNotBlank(gpxFileTextField.getText()))
                {
                    addLog("INFO", "Setting GPX start folder: " + gpxFileTextField.getText());
                    chooser.setCurrentDirectory(new File(gpxFileTextField.getText()));
                }
                else if (StringUtils.isNotBlank(photosDirTextField.getText()))
                {
                    addLog("INFO", "Setting GPX start folder to parent of: " + photosDirTextField.getText());
                    chooser.setCurrentDirectory(new File(photosDirTextField.getText()).getParentFile());
                }

                int result = chooser.showDialog(baseFrame, "Select GPX file");

                if (result == JFileChooser.APPROVE_OPTION)
                {
                    File gpxFile = chooser.getSelectedFile();
                    addLog("INFO", "User selected GPX file: " + gpxFile.toString());
                    gpxFileTextField.setText(gpxFile.toString());
                }
            }
        });

        timePhotoTextField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateButtons();
            }

            private void updateButtons()
            {
                viewTimePhotoButton.setEnabled(false);
                readCaptureTimeButton.setEnabled(false);

                String fileName = timePhotoTextField.getText().toLowerCase();

                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                {
                    File file = new File(timePhotoTextField.getText());

                    if (file.exists() && file.isFile())
                    {
                        viewTimePhotoButton.setEnabled(true);
                        readCaptureTimeButton.setEnabled(true);
                    }
                }
            }
        });

        /**
         * View thumbnails
         */
        viewTimePhotoButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addLog("INFO", "Viewing GPS time image: " + timePhotoTextField.getText());
                File timeImageFile = new File(timePhotoTextField.getText());

                if (timeImageFile.exists() && timeImageFile.isFile())
                {
                    try
                    {
                        GPSPhoto photo = new GPSPhoto(timeImageFile);
                        photo.loadMetaData();

                        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

                        BufferedImage thumbnail = Thumbnails.of(timeImageFile).size((int) (d.width * 0.9), (int) (d.height * 0.9))
                                .outputQuality(0.85).asBufferedImage();

                        JDialog imageDialog = new JDialog(baseFrame, false)
                        {
                            @Override
                            public void dispose()
                            {
                                super.dispose();
                                addLog("INFO", "Image viewer dialog was disposed");
                            }
                        };
                        imageDialog.setTitle(timeImageFile.getName());
                        imageDialog.setResizable(false);

                        JPanel imagePanel = new JPanel()
                        {
                            @Override
                            protected void paintComponent(Graphics g)
                            {
                                super.paintComponent(g);
                                g.drawImage(thumbnail, 0, 0, null);
                            }
                        };

                        JTextField dateTextField = new JTextField();
                        dateTextField.setText(photo.getFormattedCaptureTime());

                        addLog("INFO", "Read capture time from image: " + photo.getFormattedCaptureTime());

                        JPanel buttonPanel = new JPanel(new FlowLayout());
                        JButton doneButton = new JButton("Done");
                        buttonPanel.add(doneButton);
                        doneButton.addActionListener(new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                imageDialog.dispose();
                            }
                        });

                        imageDialog.getRootPane().setLayout(new BorderLayout());
                        imageDialog.getRootPane().add(dateTextField, BorderLayout.NORTH);
                        imageDialog.getRootPane().add(imagePanel, BorderLayout.CENTER);
                        imageDialog.getRootPane().add(buttonPanel, BorderLayout.SOUTH);
                        imageDialog.setSize(new Dimension(thumbnail.getWidth(), thumbnail.getHeight() + (int) buttonPanel.getPreferredSize().getHeight()));

                        imageDialog.setLocationRelativeTo(null);
                        imageDialog.setVisible(true);
                    }
                    catch (IOException e1)
                    {
                        addLog("ERROR", "Failed to create thumbnail for viewing: " + e1.toString());
                    }
                    catch (GPSPhotoException e1)
                    {
                        addLog("ERROR", "Failed to load image: " + e1.toString());
                    }
                }
            }
        });

        processButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                List<String> validationErrors = new ArrayList<>();

                validateData(validationErrors);

                if (!validationErrors.isEmpty())
                {
                    String errorMessage = StringUtils.join(validationErrors, "\n");
                    JOptionPane.showMessageDialog(baseFrame, errorMessage, "Validation failed", JOptionPane.ERROR_MESSAGE);
                    processButton.setEnabled(true);
                    return;
                }

                String kmlFile = exportKMLCheckBox.isSelected() ?
                        fileNameTextField.getText() + ".kml" : null;

                String shapeFile = exportShapeCheckBox.isSelected() ?
                        fileNameTextField.getText() + ".shp" : null;

                String gpxFile = saveGPXTrackCheckBox.isSelected() ?
                        fileNameTextField.getText() + ".gpx" : null;


                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            addLog("INFO", "Commencing processing");
                            progressBar.setValue(0);
                            GPSPhotoProcessor processor = new GPSPhotoProcessor(
                                fileNameTextField.getText(),
                                gpxFileTextField.getText(),
                                photosDirTextField.getText(),
                                thumbnailsCheckBox.isSelected(),
                                Long.parseLong(timeToleranceTextField.getText()),
                                timePhotoTextField.getText(),
                                timeTextField.getText(),
                                recursivePhotosCheckBox.isSelected(),
                                outputDirTextField.getText(),
                                kmlFile,
                                shapeFile,
                                Integer.parseInt(coresTextField.getText()));

                            if (saveGPXTrackCheckBox.isSelected())
                            {
                                FileUtils.copyFile(new File(gpxFileTextField.getText()),
                                    new File(outputDirTextField.getText(), gpxFile));

                                addLog("INFO", "Copied GPX file to: " + new File(outputDirTextField.getText(), gpxFile).toString());
                            }

                            processor.setLogFrame(baseFrame);

                            processButton.setEnabled(false);
                            processor.process();

                            addLog("INFO", "Image processing is complete: " + processor.getSummary());

                            SwingUtilities.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    setProcessingComplete(1.0);
                                }
                            });

                            JOptionPane.showMessageDialog(baseFrame, processor.getSummary(),
                                                          "Processing complete",
                                                          JOptionPane.INFORMATION_MESSAGE);
                        }
                        catch (Throwable t)
                        {
                            LOGGER.error("Failed to process images", t);

                            addLog("ERROR", "Failed to process images: " + t.toString());
                            JOptionPane.showMessageDialog(baseFrame,
                                "Failed to process images: " + t.toString(),
                                "Processing error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                        finally
                        {
                            processButton.setEnabled(true);
                        }
                    }
                };

                Thread thread = new Thread(r);
                thread.setDaemon(false);
                thread.start();
            }
        });
    }

    private void validateData(List<String> validationErrors)
    {
        if (StringUtils.isBlank(fileNameTextField.getText()))
        {
            validationErrors.add("No output file name prefix specified");
        }

        if (StringUtils.isBlank(photosDirTextField.getText()))
        {
            validationErrors.add("No input photo directory specified");
        }

        if (StringUtils.isBlank(timePhotoTextField.getText()))
        {
            validationErrors.add("No GPS photo specified");
        }

        if (StringUtils.isBlank(timeTextField.getText()))
        {
            validationErrors.add("No GPS time specified");
        }

        if (StringUtils.isBlank(gpxFileTextField.getText()))
        {
            validationErrors.add("No input GPX file specified");
        }

        if (StringUtils.isBlank(outputDirTextField.getText()))
        {
            validationErrors.add("No output directory specified");
        }

        if (StringUtils.isBlank(timeToleranceTextField.getText()))
        {
            validationErrors.add("No time tolerance specified");
        }
        else
        {
            try
            {
                long timeTolerance = Long.parseLong(timeToleranceTextField.getText());

                if (timeTolerance < 0L)
                {
                    validationErrors.add("Time tolerance must be greater than or equal to zero");
                }
            }
            catch (NumberFormatException e)
            {
                validationErrors.add("Time tolerance must be an integer");
            }
        }


    }

    public void addLog(String logType, String message)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    logTextArea.append(String.format("%s [%s] %s\n",
                        logDateFormat.format(new Date()), logType, message));
                }
            });
        }
        else
        {
            logTextArea.append(String.format("%s [%s] %s\n",
                        logDateFormat.format(new Date()), logType, message));
        }
    }

    /**
     * Handle close requests as exits
     * @param e the window event
     */
    protected void processWindowEvent(WindowEvent e)
    {
        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING)
        {
            exit();
        }
    }

    public void setProcessingComplete(double percentComplete)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressBar.setValue((int) (100 * percentComplete));
            }
        });
    }
}
