package warehouse;

import java.io.FileInputStream;
import java.util.Properties;

public final class Config {

    private final long tickMs;
    private final long maxTicks;
    private final double deliveryProb;
    private final int trolleyCapacity;
    private final int numberOfTrolleys;
    private final int numPickers; // how many picker threads to run
    private final int numStockers; // how many stocker threads to run


    public Config(long tickMs, long maxTicks, double deliveryProb,
        int trolleyCapacity, int numberOfTrolleys, int sectionCapacity, int numPickers, int numStockers) {
        this.tickMs = tickMs;
        this.maxTicks = maxTicks;
        this.deliveryProb = deliveryProb;
        this.trolleyCapacity = trolleyCapacity;
        this.numberOfTrolleys = numberOfTrolleys;
        this.sectionCapacity = sectionCapacity;
        this.numPickers = numPickers;
        this.numStockers = numStockers;
    }
    //
    private final int sectionCapacity;

    public int getSectionCapacity() {
        return sectionCapacity;
    }

    public long getTickMs() {
        return tickMs;
    }

    public long getMaxTicks() {
        return maxTicks;
    }

    public double getDeliveryProb() {
        return deliveryProb;
    }

    public int getTrolleyCapacity() {
        return trolleyCapacity;
    }

    public int getNumberOfTrolleys() {
        return numberOfTrolleys;
    }
    // getter for number of pickers
    public int getNumPickers() { 
        return numPickers; 
    }
    // getter for number of stockers
    public int getNumStockers() {
         return numStockers; 
    }

    // loading our config file
    public static Config load(String path) throws Exception {

        Properties properties = new Properties();// setting default values
        properties.setProperty("tick_ms", "100");
        properties.setProperty("max_ticks", "5000");
        properties.setProperty("delivery_prob", "0.01");
        properties.setProperty("trolley_capacity", "10");
        properties.setProperty("section_capacity", "10");
        properties.setProperty("num_pickers", "3");
        properties.setProperty("num_stockers", "2");

        if (path != null) {
            try (FileInputStream input = new FileInputStream(path)) {
                properties.load(input);// opens config file and loads the properties inside of it
            }
        }
        // parsing the file and setting properties
        long tickMs = Long.parseLong(properties.getProperty("tick_ms"));
        long maxTicks = Long.parseLong(properties.getProperty("max_ticks"));
        double deliveryProb = Double.parseDouble(properties.getProperty("delivery_prob"));
        int capacity = Integer.parseInt(properties.getProperty("trolley_capacity"));
        int sectionCapacity = Integer.parseInt(properties.getProperty("section_capacity"));
        int numPickers = Integer.parseInt(properties.getProperty("num_pickers"));
        int numStockers = Integer.parseInt(properties.getProperty("num_stockers"));

        // K = number of trolleys
        // if K is missing or blank, use average of pickers and stockers
        String kValue = properties.getProperty("K");
        int numberOfTrolleys = (kValue == null || kValue.isBlank())
                ? (numPickers + numStockers) / 2
                : Integer.parseInt(kValue);

        return new Config(tickMs, maxTicks, deliveryProb, capacity, numberOfTrolleys, sectionCapacity, numPickers, numStockers);
    }
}