package com.rhis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.Collection;
import java.util.Map;

/**
 * Exports RHIS as a single CSV
 */
public class RHISExporter
{
    private static final Logger LOGGER = Logger.getLogger(RHISExporter.class);

    private String inputDirectory;
    private final boolean recursive;
    private final String outputFile;

    private Workbook wb = new HSSFWorkbook();
    private Sheet sheet = wb.createSheet("RHIS data");
    private Row headerRow = sheet.createRow(0);
    private int rowNumber = 1;
    private int maxColumns = 0;

    public RHISExporter(String inputDirectory, boolean recursive, String outputFile)
    {
        this.inputDirectory = inputDirectory;
        this.recursive = recursive;
        this.outputFile = outputFile;
    }

    public int export() throws IOException, CompressorException
    {
        Collection<File> surveyFiles = FileUtils.listFiles(new File(inputDirectory), new String[] { "survey" }, recursive);

        for (File survey: surveyFiles)
        {
            LOGGER.info("Found survey: " + survey);
            String json = loadCompressedJson(survey);
            processJson(json);
        }

        makeColumnsNumeric();

        FileOutputStream out = new FileOutputStream(outputFile);
        wb.write(out);
        out.close();

        return surveyFiles.size();

    }

    public boolean isDouble(String value)
    {
        if (StringUtils.isBlank(value))
        {
            return true;
        }

        try
        {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException n)
        {
            return false;
        }
    }
    private void makeColumnsNumeric()
    {
        for (int column = 0; column < maxColumns; ++column)
        {
            boolean allNumeric = true;

            for (int row = 1; row < rowNumber; ++row)
            {
                String value = sheet.getRow(row).getCell(column).getStringCellValue();

                if (StringUtils.isNotBlank(value) && !isDouble(value))
                {
                    allNumeric = false;
                    break;
                }
            }

            if (allNumeric)
            {
                CellStyle numberStyle = wb.createCellStyle();
                numberStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.00"));

                for (int row = 1; row < rowNumber; ++row)
                {
                    Cell cell = sheet.getRow(row).getCell(column);
                    cell.setCellStyle(numberStyle);

                    String value = cell.getStringCellValue();

                    if (StringUtils.isNotBlank(value))
                    {
                        cell.setCellValue(Double.parseDouble(value));
                    }
                }
            }
        }
    }

    private void processJson(String json)
    {
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(json);
        JsonObject jsonObject = jsonTree.getAsJsonObject();

        int columnNumber = 0;
        Row dataRow = sheet.createRow(rowNumber++);

        for (Map.Entry<String,JsonElement> entry : jsonObject.entrySet())
        {
            if (entry.getKey().equals("type") || entry.getKey().equals("version") || entry.getKey().equals("surveyid"))
            {
                continue;
            }

            JsonArray arrayElement = entry.getValue().getAsJsonArray();
            columnNumber = extractSheetDetails(dataRow, headerRow, arrayElement, columnNumber);

            maxColumns = Math.max(maxColumns, columnNumber);
            // Null out after first survey
        }

        headerRow = null;
    }

    private int extractSheetDetails(Row row, Row headerRow, JsonArray element, int columnNumber)
    {
        int headerColumn = columnNumber;
        int dataColumn = columnNumber;

        for (int i = 0; i < element.size(); ++i)
        {
            JsonObject entryObject = element.get(i).getAsJsonObject();

            for (Map.Entry<String,JsonElement> entry : entryObject.entrySet())
            {
                if (entry.getKey().equals("additionalinfo"))
                {
                    continue;
                }
                if (entry.getKey().equals("gps"))
                {
                    if (headerRow != null)
                    {
                        Cell cell = headerRow.createCell(headerColumn++);
                        cell.setCellValue("LongWGS84");
                        cell = headerRow.createCell(headerColumn++);
                        cell.setCellValue("LatWGS84");
                    }

                    Pair latLong = splitLatLon(getStringValue(entry.getValue()));
                    Cell cell = row.createCell(dataColumn++);
                    cell.setCellValue(latLong.getKey());
                    cell = row.createCell(dataColumn++);
                    cell.setCellValue(latLong.getValue());
                }
                else
                {
                    if (headerRow != null)
                    {
                        Cell cell = headerRow.createCell(headerColumn++);
                        cell.setCellValue(entry.getKey());
                    }

                    Cell cell = row.createCell(dataColumn++);
                    cell.setCellValue(getStringValue(entry.getValue()));
                }
            }
        }

        return dataColumn;
    }

    private String getStringValue(JsonElement value)
    {
        if (value.isJsonNull())
        {
            return "";
        }
        else if (value.isJsonPrimitive())
        {
            return value.getAsString().trim();
        }

        throw new IllegalStateException("Unhandled data type while extracting values as String");
    }

    private Pair splitLatLon(String latLon)
    {
        String [] split = latLon.split(",");
        return new Pair(split[0].trim(), split[1].trim());
    }

    private String loadCompressedJson(File survey) throws IOException, CompressorException
    {
        ZipFile zipFile = new ZipFile(survey);
        ZipArchiveEntry surveyEntry = zipFile.getEntry("survey.json");
        InputStream in = zipFile.getInputStream(surveyEntry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        in.close();
        return new String(out.toByteArray(), "UTF-8");
    }

    public static void main(String [] args)
    {
        CommandLine commands = parseCommandLine(args);

        String inputDir = commands.getOptionValue("inputdir");
        boolean recursive = commands.hasOption("recursive");
        String outputFile = commands.getOptionValue("outputfile");

        RHISExporter exporter = new RHISExporter(inputDir, recursive, outputFile);

        try
        {
            int count = exporter.export();
            LOGGER.info(String.format("Successfully exported: [%d] surveys", count));

        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to export RHIS surveys", t);
            System.exit(1);
        }

    }

    private static CommandLine parseCommandLine(String[] args)
    {
        Options options = new Options();

        options.addOption(Option.builder("inputdir")
              .argName("input survey folder")
              .hasArg().required(true)
              .desc("folder containing surveys to export").build());

        options.addOption(Option.builder("recursive")
              .argName("recursive survey search")
              .desc("search recursively for surveys").build());

        options.addOption(Option.builder("outputfile")
              .argName("output file")
              .hasArg().required(true)
              .desc("output Excel result file").build());

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
        formatter.printHelp("RHISExporter", options);
    }
}
