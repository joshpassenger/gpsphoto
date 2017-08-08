package com.gpsphoto;

import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug reader for shape files
 */
public class ReadShapeFile
{
    private static final Logger LOGGER = Logger.getLogger(ReadShapeFile.class);

    public static void main(String [] args) throws IOException, GPSPhotoException
    {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        File inputShapeFile = new File("data/trial2/results/", "mission.shp");

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", inputShapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createDataStore(params);

        Transaction transaction = new DefaultTransaction("read");

        SimpleFeatureSource featureSource = newDataStore.getFeatureSource();

        if (featureSource instanceof SimpleFeatureStore)
        {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try
            {
                SimpleFeatureCollection features = featureStore.getFeatures();

                SimpleFeatureIterator it = features.features();

                while(it.hasNext())
                {
                    SimpleFeature feature = it.next();
                    LOGGER.info("Found feature: " + feature.toString());

                }
                transaction.commit();

            } catch (Throwable t)
            {
                transaction.rollback();
                throw new GPSPhotoException("Failed to read shape file", t);
            }
            finally
            {
                transaction.close();
            }
        }
    }
}
