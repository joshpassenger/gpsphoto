package com.gpsphoto;

import com.hs.gpxparser.GPXParser;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.TrackSegment;
import com.hs.gpxparser.modal.Waypoint;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.xml.XMLUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class GPSPhotoProcessor
{
    private static final Logger LOGGER = Logger.getLogger(GPSPhotoProcessor.class);

    private final String projectName;

    private final String gpxFile;
    private GPX gpxData;
    private final String photosDir;
    private final boolean recursive;

    private final boolean thumbnails;
    private String timePhoto;

    private String photoTime;
    private final long tolerance;

    private long timeOffset = 0L;

    private final String outputDir;

    private final int cores;

    private final String kmlFile;
    private final String shapeFile;

    private List<GPSPhoto> photos = new ArrayList<>();
    private List<GPSPosition> positions = new ArrayList<>();
    private GPSPhotoFrame logFrame = null;
    private String summary;

    private final Properties properties = new Properties();

    private FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public GPSPhotoProcessor(
                String projectName,
                String gpxFile,
                String photosDir,
                boolean thumbnails,
                long tolerance,
                String timePhoto,
                String photoTime,
                boolean recursive,
                String outputDir,
                String kmlFile, // may be null
                String shapeFile,
                int cores) throws GPSPhotoException, IOException
    {
        this.projectName = projectName;
        this.gpxFile = gpxFile;
        this.photosDir = photosDir;
        this.thumbnails = thumbnails;
        this.tolerance = tolerance;
        this.timePhoto = timePhoto;
        this.photoTime = photoTime;
        this.recursive = recursive;
        this.outputDir = outputDir;
        this.kmlFile = kmlFile;
        this.shapeFile = shapeFile;
        this.cores = cores;

        if (Objects.equals(photosDir, outputDir))
        {
            throw new GPSPhotoException("Attempt made to write to input photo directory");
        }

        File outputDirFile = new File(outputDir);

        if (outputDirFile.exists() && outputDirFile.isFile())
        {
            throw new GPSPhotoException("Output directory is a file: " + outputDir);
        }

        if (!outputDirFile.exists())
        {
            outputDirFile.mkdirs();
        }

        loadProperties();
	}

    private void loadProperties() throws GPSPhotoException
    {
        try (InputStream in = getClass().getResourceAsStream("/config/GPSPhotoProcessor.properties"))
        {
            properties.load(in);
        }
        catch (Throwable t)
        {
            throw new GPSPhotoException("Failed to load system properties", t);
        }
    }

    private void loadGPX()
    {
        try
        {
            GPXParser p = new GPXParser();
            FileInputStream in = new FileInputStream(gpxFile);
            gpxData = p.parseGPX(in);

            LOGGER.info(String.format("Successfully parsed GPX file: [%s]", gpxFile));
        }
        catch (Throwable t)
        {
            LOGGER.error(String.format("Failed to parse GPX file from: [%s]", gpxFile), t);
        }
    }

	public static void main(String [] args)
	{
        long start = System.currentTimeMillis();

        try
        {
            CommandLine commands = parseCommandLine(args);

            /**
             * Extract parameters
             */
            String projectName = commands.getOptionValue("projectname");
            String gpxFile = commands.getOptionValue("gpx");
            String photosDir = commands.getOptionValue("photos");
            boolean recursive = commands.hasOption("recursive");
            boolean thumbnails = commands.hasOption("thumbnails");
            String timePhoto = commands.getOptionValue("timephoto");
            String photoTime = commands.getOptionValue("phototime");
            String outputDir = commands.getOptionValue("outputdir");
            long tolerance = 2000L;

            /**
             * TODO Add distance tolerance
             */

            String kmlFile = null;
            String shapeFile = null;

            if (commands.hasOption("kmlfile"))
            {
                kmlFile = commands.getOptionValue("kmlfile");
            }

            if (commands.hasOption("shapefile"))
            {
                shapeFile = commands.getOptionValue("shapefile");
            }

            int cores = Runtime.getRuntime().availableProcessors();

            if (commands.hasOption("cores"))
            {
                try
                {
                    cores = Integer.parseInt(commands.getOptionValue("cores"));

                    if (cores <= 0)
                    {
                        throw new GPSPhotoException("Invalid core count: " + cores);
                    }
                }
                catch (NumberFormatException n)
                {
                    throw new GPSPhotoException("Invalid core count: " + commands.getOptionValue("cores"));
                }
            }

            if (commands.hasOption("tolerance"))
            {
                try
                {
                    tolerance = Long.parseLong(commands.getOptionValue("tolerance"));

                    if (tolerance <= 0)
                    {
                        throw new GPSPhotoException("Invalid tolerance: " + tolerance);
                    }
                }
                catch (NumberFormatException n)
                {
                    throw new GPSPhotoException("Invalid tolerance: " + commands.getOptionValue("tolerance"));
                }
            }

            /**
             * Create the worker
             */
            GPSPhotoProcessor gpsPhotoProcessor = new GPSPhotoProcessor(
                    projectName,
                    gpxFile,
                    photosDir,
                    thumbnails,
                    tolerance,
                    timePhoto,
                    photoTime,
                    recursive,
                    outputDir,
                    kmlFile,
                    shapeFile,
                    cores);

            gpsPhotoProcessor.process();
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to process GPS photos: " + t.toString(), t);
            System.exit(1);
        }

        long end = System.currentTimeMillis();
        int seconds = (int) ((end - start) / 1000L);
        LOGGER.info(String.format("Image processing complete taking: [%d] seconds", seconds));

        System.exit(0);

    }

    public void process() throws GPSPhotoException, IOException
    {
        /**
         * Parse the GPX file
         */
        loadGPX();

        /**
         * Compute the positions and their local time offsets
         */
        computePositions();

        /**
         * Compute the time offset
         */
        computeTimeOffset();

        /**
         * Find all of the photos
         */
        processPhotos();

        /**
         * Produce an optional kml file
         */
        createKMLFile();

        /**
         * Produce an optional shape file
         */
        createShapeFile();
    }

    private SimpleFeatureType createFeatureType()
    {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setSRS("EPSG:4326"); //DefaultGeographicCRS.WGS84);

        builder.add("the_geom", Point.class);
        builder.length(100).add("Name", String.class);

        if (thumbnails)
        {
            builder.length(100).add("Image", String.class);
        }

        builder.length(20).add("Date", String.class);

        builder.add("Latitude", Double.class);
        builder.add("Longitude", Double.class);

        return builder.buildFeatureType();
    }

    private void createShapeFile() throws GPSPhotoException, IOException
    {
        if (StringUtils.isBlank(shapeFile))
        {
            return;
        }

        FastDateFormat fdf = FastDateFormat.getInstance("dd/MM/yyyy HH:mm:ss");

        SimpleFeatureType featureType = createFeatureType();

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        DefaultFeatureCollection collection = new DefaultFeatureCollection("Photos");

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        for (GPSPhoto photo: photos)
        {
            if (!photo.isMatched())
            {
                continue;
            }

            String name = FilenameUtils.removeExtension(photo.getInputFile().getName());
            Point point = geometryFactory.createPoint(new Coordinate(photo.getLongitude(), photo.getLatitude()));

            builder.set("the_geom", point);
            builder.set("Name", name);

            if (thumbnails)
            {
                builder.set("Image", photo.getInputFile().getName());
            }

            builder.set("Date", fdf.format(photo.getOffsetTime()));
            builder.set("Latitude", photo.getLatitude());
            builder.set("Longitude", photo.getLongitude());

            SimpleFeature feature = builder.buildFeature(name);
            collection.add(feature);
        }


        File outputShapeFile = new File(outputDir, shapeFile);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", outputShapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore)
        {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try
            {
                featureStore.addFeatures(collection);
                transaction.commit();

            } catch (Throwable t)
            {
                transaction.rollback();
                throw new GPSPhotoException("Failed to write shape file", t);
            }
            finally
            {
                transaction.close();
            }
        }

        LOGGER.info("Shape file written to: " + outputShapeFile);
    }

    private void createKMLFile() throws IOException
    {
        if (StringUtils.isBlank(kmlFile))
        {
            return;
        }

        StringBuilder buf = new StringBuilder();

        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buf.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\"");
        buf.append(" xmlns:gx=\"http://www.google.com/kml/ext/2.2\"");
        buf.append(" xmlns:kml=\"http://www.opengis.net/kml/2.2\"");
        buf.append(" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        buf.append("<Document>\\n");
        buf.append("	<name>").append(XMLUtils.removeXMLInvalidChars(projectName)).append("</name>\n");
        buf.append("	<Style id=\"sn_placemark_circle\">\n");
        buf.append("		<IconStyle>\n");
        buf.append("			<color>ff00ffff</color>\n");
        buf.append("			<scale>0.8</scale>\n");
        buf.append("			<Icon>\n");
        buf.append("				<href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n");
        buf.append("			</Icon>\n");
        buf.append("		</IconStyle>\\n");
		buf.append("		<LabelStyle>\n");
		buf.append("            <scale>0</scale>\n");
		buf.append("        </LabelStyle>\n");
        buf.append("		<ListStyle>\n");
        buf.append("		</ListStyle>\n");
        buf.append("	</Style>\n");
        buf.append("	<StyleMap id=\"msn_placemark_circle\">\n");
        buf.append("		<Pair>\n");
        buf.append("			<key>normal</key>\n");
        buf.append("			<styleUrl>#sn_placemark_circle</styleUrl>\n");
        buf.append("		</Pair>\n");
        buf.append("		<Pair>\n");
        buf.append("			<key>highlight</key>\n");
        buf.append("			<styleUrl>#sh_placemark_circle_highlight</styleUrl>\n");
        buf.append("		</Pair>\n");
        buf.append("	</StyleMap>\n");
        buf.append("	<Style id=\"sh_placemark_circle_highlight\">\n");
        buf.append("		<IconStyle>\n");
        buf.append("			<color>ff00ffff</color>\n");
        buf.append("			<scale>1.0</scale>\n");
        buf.append("			<Icon>\n");
        buf.append("				<href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n");
        buf.append("			</Icon>\n");
        buf.append("		</IconStyle>\n");
        buf.append("		<ListStyle>\n");
        buf.append("		</ListStyle>\n");
        buf.append("	</Style>\n");

        for (GPSPhoto photo: photos)
        {
            if (!photo.isMatched())
            {
                continue;
            }

            String encodedFileName = XMLUtils.removeXMLInvalidChars(
                    FilenameUtils.removeExtension(photo.getInputFile().getName()));

            buf.append("	<Placemark>\n");
            buf.append("		<name>").append(encodedFileName).append("</name>\n");
            buf.append("		<description><![CDATA[<img src=\"").append(photo.getInputFile().getName()).append("\"/>]]></description>\n");
            buf.append("		<LookAt>\n");
            buf.append("			<longitude>").append(String.format("%.16f", photo.getLongitude())).append("</longitude>\n");
            buf.append("			<latitude>").append(String.format("%.16f", photo.getLatitude())).append("</latitude>\n");
            buf.append("			<altitude>0</altitude>\n");
            buf.append("			<heading>0</heading>\n");
            buf.append("			<tilt>0</tilt>\n");
            buf.append("			<range>300</range>\n");
            buf.append("			<gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>\n");
            buf.append("		</LookAt>\n");
            buf.append("		<styleUrl>#msn_placemark_circle</styleUrl>\n");
            buf.append("		<Point>\n");
            buf.append("			<gx:drawOrder>1</gx:drawOrder>\n");
            buf.append("			<coordinates>").append(String.format("%.16f,%.16f,0", photo.getLongitude(), photo.getLatitude())).append("</coordinates>\n");
            buf.append("		</Point>\n");
            buf.append("	</Placemark>\n");
        }

        buf.append("</Document>\n");
        buf.append("</kml>\n");


        File outputKMLFile = new File(outputDir, kmlFile);
        FileUtils.write(outputKMLFile, buf.toString());

        LOGGER.info("Saved kml file to: " + outputKMLFile);
    }

    private void computePositions()
    {
        /**
         * Initial support for tracks
         */
        for (Track track: gpxData.getTracks())
        {
            for (TrackSegment segment: track.getTrackSegments())
            {
                for (Waypoint waypoint: segment.getWaypoints())
                {
                    GPSPosition position =
                            new GPSPosition(
                                    waypoint.getLatitude(),
                                    waypoint.getLongitude(),
                                    waypoint.getTime());

                    positions.add(position);
                }
            }
        }

        Collections.sort(positions);



        if (logFrame != null)
        {
            logFrame.addLog("INFO", String.format("Loaded [%d] GPS positions", positions.size()));
            if (!positions.isEmpty())
            {
                Date minDate = positions.get(0).getLocalTime();
                Date maxDate = positions.get(positions.size() - 1).getLocalTime();
                logFrame.addLog("INFO",
                    String.format("Minimum GPS local time: [%s] Maximum GPS local time: [%s]",
                                  dateFormat.format(minDate), dateFormat.format(maxDate)));
            }
        }
    }

    private void computeTimeOffset() throws GPSPhotoException
    {
        GPSPhoto photo = new GPSPhoto(new File(timePhoto));
        photo.loadMetaData();

        Date gpsDateTime;

        try
        {
            gpsDateTime = dateFormat.parse(photoTime);
            Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
            calendar.setTime(gpsDateTime);
            gpsDateTime = calendar.getTime();
        }
        catch (Throwable t)
        {
            throw new GPSPhotoException("Failed to parse input data must be in format: yyyy-MM-dd HH:mm:ss found: " + photoTime, t);
        }

        timeOffset = gpsDateTime.getTime() - photo.getCaptureTime().getTime();

        LOGGER.info(String.format("Found time difference: [%d] milliseconds", timeOffset));

        if (logFrame != null)
        {
            logFrame.addLog("INFO", String.format("Found time difference: [%d] milliseconds", timeOffset));
        }
    }

    /**
     * Identifies and loads photos making thumbnails
     */
    private void processPhotos() throws GPSPhotoException
    {
        Collection<File> photoFiles = FileUtils.listFiles(new File(photosDir), new String[] { "jpg", "jpeg", "JPG", "JPEG" }, recursive);

        LOGGER.info(String.format("Found: [%d] matching input files to process", photoFiles.size()));

        for (File photoFile: photoFiles)
        {
            GPSPhoto photo = new GPSPhoto(photoFile);
            if (thumbnails)
            {
                photo.setThumbnailFile(new File(outputDir, photoFile.getName()));
            }

            photo.loadMetaData();
            photo.applyTimeDelta(timeOffset);
            photos.add(photo);
        }

        Collections.sort(photos, new Comparator<GPSPhoto>()
        {
            @Override
            public int compare(GPSPhoto photo1, GPSPhoto photo2)
            {
                return photo1.getCaptureTime().compareTo(photo2.getCaptureTime());
            }
        });

        LinkedBlockingQueue<GPSPhoto> photosToProcess = new LinkedBlockingQueue<>(photos);
        LinkedBlockingQueue<GPSPhoto> completePhotos = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<GPSPhoto> failedPhotos = new LinkedBlockingQueue<>();

        LOGGER.info(String.format("Processing photos using: [%d] cores with tolerance: [%d] milliseconds",
                  cores, tolerance));

        final List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < cores; ++i)
        {
            GPSPhotoThread runnable = new GPSPhotoThread(positions, tolerance, photosToProcess, completePhotos, failedPhotos);
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.start();
            threads.add(thread);
        }

        while (!photosToProcess.isEmpty())
        {
            double totalCount = photos.size();
            double processedCount = completePhotos.size() + failedPhotos.size();

            double percentComplete = totalCount == 0 ? 1.0 : processedCount / totalCount;

            if (logFrame != null)
            {
                logFrame.setProcessingComplete(percentComplete);
            }

            sleep(100L);
        }

        /**
         * Wait for all of the threads to complete
         */
        for (Thread thread: threads)
        {
            try
            {
                thread.join();
            }
            catch (InterruptedException ignored)
            {
            }
        }

        this.summary = String.format("Georeferenced: [%d] photos and rejected: [%d] photos",
                                  completePhotos.size(), failedPhotos.size());
        LOGGER.info(String.format("Georeferenced: [%d] photos and rejected: [%d] photos",
                                  completePhotos.size(), failedPhotos.size()));

        if (logFrame != null)
        {
            while (!failedPhotos.isEmpty())
            {
                GPSPhoto photo = failedPhotos.poll();

                if (photo != null)
                {
                    String message = String.format("Failed to position photo: [%s] local time: [%s] offset time: [%s]",
                        photo.getInputFile().toString(),
                        dateFormat.format(photo.getCaptureTime()),
                        dateFormat.format(photo.getOffsetTime()));
                    logFrame.addLog("WARNING", message);
                }
            }
        }


    }

    private void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException ignored)
        {
        }
    }

    private static CommandLine parseCommandLine(String[] args)
    {
        Options options = new Options();

        options.addOption(Option.builder("projectname")
              .hasArg().argName("Project name")
              .required(true).desc("Project name used to name output files").build());

        options.addOption(Option.builder("gpx")
              .hasArg().argName("input GPX file")
              .required(true).desc("Input GPX file").build());

        options.addOption(Option.builder("photos")
              .argName("input photo folder")
              .hasArg().required(true)
              .desc("folder containing photos to process").build());

        options.addOption(Option.builder("recursive")
              .argName("recursive photo search")
              .desc("search recursively for photos").build());

        options.addOption(Option.builder("cores")
              .argName("cores to use").hasArg()
              .desc("the number of CPU cores to use for photo matching").build());

        options.addOption(Option.builder("outputdir")
              .argName("output directory").required().hasArg()
              .desc("output directory to write results to").build());

        options.addOption(Option.builder("thumbnails")
              .argName("output thumbnails")
              .desc("optionally output thumbnails to the output directory").build());

        options.addOption(Option.builder("timephoto")
              .argName("timestamp photo")
              .hasArg().required(true)
              .desc("photo with known GPS timestamp").build());

        options.addOption(Option.builder("phototime")
              .argName("photo timestamp")
              .hasArg().required(true)
              .desc("time of timestamp photo").build());

        options.addOption(Option.builder("tolerance")
              .argName("time matching tolerance in milliseconds")
              .hasArg()
              .desc("tolerance in seconds for GPS points to be considered [2000] milliseconds").build());

        options.addOption(Option.builder("shapefile")
              .argName("shape file name").hasArg().required(false)
              .desc("name of the shape file to write in the output directory").build());

        options.addOption(Option.builder("kmlfile")
              .argName("kml file name").hasArg().required(false)
              .desc("name of the kml file to write in the output directory").build());

        options.addOption(Option.builder("help").required(false)
              .desc("show help information").build());

        CommandLine cmd = null;

        try
        {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);

            if (cmd.hasOption("help"))
            {
                LOGGER.info("Help was requested");
                showHelp(options);
                System.exit(1);
            }
        }
        catch (ParseException e)
        {
            LOGGER.error("Failed to parse command line: " + e.getMessage());
            showHelp(options);
            System.exit(1);
        }

        return cmd;
    }

    private static void showHelp(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("GPSPhoto", options);
    }

    public void setLogFrame(GPSPhotoFrame logFrame)
    {
        this.logFrame = logFrame;
    }

    public String getSummary()
    {
        return summary;
    }
}