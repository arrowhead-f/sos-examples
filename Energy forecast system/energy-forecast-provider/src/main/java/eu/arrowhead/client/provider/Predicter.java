package eu.arrowhead.client.provider;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import eu.arrowhead.client.common.can_be_modified.model.Entry;
import org.joda.time.DateTime;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Predicter {
    private static final String CACHE_MODEL_FMT = "cache%d.model";
    private static final String CACHE_CSV_FMT = "cache%d.csv";
    private static final String CACHE_TS = "cache_ts.csv";
    private static final Logger LOG = Logger.getLogger(Predicter.class.getName());

    private static Long[] tsData = null;

    private final String cacheModel;
    private final String cacheCSV;

    private List<String[]> csvData;
    private Classifier heatModel;
    private float[] waterModel = new float[24];

    public static float predictWaterUsage(long building, int hour) throws Exception {
        return new Predicter(building).predictWaterUsage(hour);
    }

    public static float predictTotalUsage(long building, double outTemp) throws Exception {
        return new Predicter(building).predictTotalUsage(outTemp);
    }

    public static void update(List<Entry> entries) throws Exception {
        if (tsData == null) readTSCache();

        Map<Long, Predicter> predicters = new TreeMap<>();

        final int len = entries.size();
        int i = 0;
        for (Entry entry : entries) {
            i++;
            if (i % 1000 == 0) {
                LOG.info("Entry " + i + "/" + len + "...");
            }
            final Long building = entry.getBuilding();
            final Long timestamp = entry.getTimestamp();

            if (building == null) {
                LOG.warning("No building ID in entry, skipping...");
                continue;
            }

            if (!predicters.containsKey(building)) predicters.put(building, new Predicter(building));

            if (entry.getOutTemp() != null || entry.getTotal() != null || entry.getWater() != null)
                predicters.get(building).addUsage(entry);

            if (entry.getOutTemp() != null) {
                if (tsData[0] == null || timestamp > tsData[0]) tsData[0] = timestamp;
            }

            if (entry.getTotal() != null || entry.getWater() != null) {
                if (tsData[1] == null || timestamp > tsData[1]) tsData[1] = timestamp;
            }

            if (entry.getInTemp() != null) {
                if (tsData[2] == null || timestamp > tsData[2]) tsData[2] = timestamp;
                predicters.get(building).addIndoorTemp(entry);
            }
        }

        predicters.forEach((building, predicter) -> {
            try {
                predicter.writeCSVCache();
                predicter.recalcHeatModel();
                predicter.recalcWaterModel();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Updating predicter failed: ", e);
            }
        });

        writeTSCache();
    }

    public static long lastWeatherTimeStamp() {
        if (tsData == null) readTSCache();
        if (tsData[0] != null) return tsData[0];
        else return 0;
    }

    public static long lastConsumptionTimeStamp() {
        if (tsData == null) readTSCache();
        if (tsData[1] != null) return tsData[1];
        else return 0;
    }

    public static long lastIndoorTimeStamp() {
        if (tsData == null) readTSCache();
        if (tsData[2] != null) return tsData[2];
        else return 0;
    }

    private void addIndoorTemp(Entry entry) {
        String timeStampOfEntry = entry.getTimestamp().toString();
        timeStampOfEntry = timeStampOfEntry.substring(0, timeStampOfEntry.length() - 2);
        boolean timestampExists = false; // check if the timestamp already exists
        int indexOfExistingTimestep = -1;
        for (int j = 0; j < csvData.size(); j++)
            if (csvData.get(j)[0].equals(timeStampOfEntry)) {
                timestampExists = true;
                indexOfExistingTimestep = j;
            }
        if (!timestampExists) {
            // It should always exist, unless the data was incomplete
            LOG.info("No existing entry found, skipping...");
        } else {
            if (entry.getInTemp() != null)
                csvData.get(indexOfExistingTimestep)[1] = entry.getInTemp().toString();

        }
    }

    private void addUsage(Entry entry) {
        String timeStampOfEntry = entry.getTimestamp().toString();
        if (timeStampOfEntry.length() < 3) timeStampOfEntry = "00" + timeStampOfEntry;
        timeStampOfEntry = timeStampOfEntry.substring(0, timeStampOfEntry.length() - 2);
        boolean timestampExists = false; // check if the timestamp already exists
        for (String[] record : csvData)
            if (record[0].equals(timeStampOfEntry)) {
                timestampExists = true;
            }
        if (!timestampExists) {
            // if all necessary information was sent, add them to the records
            if (entry.getOutTemp() != null && entry.getTotal() != null && entry.getWater() != null) {
                String[] currentRecord = new String[8];
                currentRecord[0] = timeStampOfEntry;
                currentRecord[2] = entry.getOutTemp().toString();
                currentRecord[3] = entry.getTotal().toString();
                currentRecord[4] = entry.getWater().toString();
                currentRecord[5] = "" + Math.abs(entry.getTotal() - entry.getWater());
                currentRecord[6] = "FALSE";
                currentRecord[7] = "FALSE";
                // If the last record was incomplete, remove this one as it is unreliable
                if (csvData.size() == 0 || csvData.get(csvData.size() - 1)[7].equals("FALSE"))
                    csvData.add(currentRecord);
                else if (csvData.size() > 0) csvData.get(csvData.size() - 1)[7] = "FALSE";
            }
            // if getTotal or getWater was not sent, we need to delete the last record as it is unreliable.
            if (entry.getTotal() == null || entry.getWater() == null) {
                // only if the last entry has not been deleted yet
                if (csvData.size() > 0 && csvData.get(csvData.size() - 1)[6].equals("FALSE"))
                {
                    csvData.remove(csvData.size() - 1);
                    csvData.get(csvData.size() - 1)[6] = "TRUE";
                    csvData.get(csvData.size() - 1)[7] = "TRUE";
                }
            }
            // if only the temperature is missing, we ignore the current entry
            // (but set the deleting of the next entry to false)
            else if (entry.getOutTemp() == null) {
                if (csvData.size() > 0)
                    csvData.get(csvData.size() - 1)[7] = "FALSE";
            }
        } else {
            LOG.warning("Duplicated timestamp found, skipping...");
        }
    }

    private float predictTotalUsage(double outTemp) throws Exception {
        if (heatModel != null) {
            Instance instance = new DenseInstance(2);
            instance.setValue(0, outTemp); // Temperature
            return (float) heatModel.classifyInstance(instance);
        } else {
            throw new NullPointerException("Learner not initialised");
        }
    }

    private float predictWaterUsage(int hour) {
        return waterModel[hour];
    }

    private Predicter(long building) throws Exception {
        cacheModel = String.format(CACHE_MODEL_FMT, building);
        cacheCSV = String.format(CACHE_CSV_FMT, building);

        readCSVCache();
        readModelCache();

        recalcWaterModel();
        if (heatModel == null) recalcHeatModel(); // Only recalc if we couldn't load cache
    }

    private static void readTSCache() {
        try {
            CSVReader reader = new CSVReader(new FileReader(CACHE_TS), ',');
            final String[] strings = reader.readNext();
            tsData = new Long[strings.length];
            for (int i = 0; i < strings.length; i++) tsData[i] = Long.parseLong(strings[i]);
            reader.close();
        } catch (IOException ignored) {}

        if (tsData == null) tsData = new Long[3];
    }

    private void readCSVCache() throws IOException {
        // Create CSV cache file if necessary and add header
        File file = new File(cacheCSV);
        if (file.createNewFile()) {
            FileWriter writer = new FileWriter(file);
            writer.write("\"DATE\",\"Indoor\",\"Outdoor\",\"Energy Consumption kWh HEAT\"," +
                    "\"ENERGY Consumption kWh Water\",\"Difference greater 0\",\"NextEntryWasIncomplete\"," +
                    "\"PreviousEntryWasIncomplete\"\n");
            writer.close();
        }

        // Load CSV cache file
        CSVReader reader = new CSVReader(new FileReader(cacheCSV), ',');
        csvData = reader.readAll();
        reader.close();
    }

    private void readModelCache() {
        try {
            heatModel = (Classifier) SerializationHelper.read(cacheModel);
        } catch (Exception e) {
            heatModel = null;
        }
    }

    private void recalcWaterModel() {
        final int[] count = new int[24];
        final float[] total = new float[24];
        for (String[] record : csvData) {
            try {
                final long timestamp = Long.parseLong(record[0]) * 100;
                final float water = Float.parseFloat(record[4]);
                final DateTime dateTime = new DateTime(timestamp * 1000);
                final int hour = dateTime.getHourOfDay();
                count[hour]++;
                total[hour] += water;
            } catch (NumberFormatException ignored) {}
        }
        for (int i = 0; i < 24; i++)
            waterModel[i] = total[i]/count[i];
    }

    private void recalcHeatModel() throws Exception {
        if (csvData.size() > 1) {
            trainHeatModel(new ConverterUtils.DataSource(cacheCSV).getDataSet(), new int[]{2, 5});
            writeModelCache();
        }
    }

    private void writeModelCache() throws Exception {
        SerializationHelper.write(cacheModel, heatModel);
    }

    private void trainHeatModel(Instances data, int[] indices) throws Exception {
        Remove r = new Remove();
        Instances res = new Instances(data);
        r.setAttributeIndicesArray(indices); // This must be before setting inputformat
        r.setInvertSelection(true);
        try {
            r.setInputFormat(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            res = Filter.useFilter(res, r);
        } catch (Exception e) {
            e.printStackTrace();
        }

        res.setClassIndex(res.numAttributes() - 1); // class label must be last
        heatModel = new MultilayerPerceptron();
        heatModel.buildClassifier(res);
    }

    private void writeCSVCache() throws IOException {
        FileWriter writer = new FileWriter(cacheCSV);
        //using custom delimiter and quote character
        CSVWriter csvWriter = new CSVWriter(writer, ',');
        csvWriter.writeAll(csvData, false);
        csvWriter.close();
    }

    private static void writeTSCache() throws IOException {
        FileWriter writer = new FileWriter(CACHE_TS);
        //using custom delimiter and quote character
        CSVWriter csvWriter = new CSVWriter(writer, ',');
        String[] strings = new String[tsData.length];
        for (int i = 0; i < strings.length; i++) strings[i] = tsData[i] == null ? "0" : Long.toString(tsData[i]);
        csvWriter.writeNext(strings, false);
        csvWriter.close();
    }
}
