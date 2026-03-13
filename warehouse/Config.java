package warehouse;

import java.io.FileInputStream;
import java.util.Properties;

public final class Config {

    private final long tickMs;
    private final long maxTicks;
    private final double deliveryProb;
    private final int trolleyCapacity;
    private final int numberOfTrolleys;

    public Config(long tickMs, long maxTicks, double deliveryProb,
            int trolleyCapacity, int numberOfTrolleys) {
        this.tickMs = tickMs;
        this.maxTicks = maxTicks;
        this.deliveryProb = deliveryProb;
        this.trolleyCapacity = trolleyCapacity;
        this.numberOfTrolleys = numberOfTrolleys;
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

    // loading our config file
    public static Config load(String path) throws Exception {

        Properties properties = new Properties();// setting default values
        properties.setProperty("tick_ms", "100");
        properties.setProperty("max_ticks", "5000");
        properties.setProperty("delivery_prob", "0.01");
        properties.setProperty("trolley_capacity", "10");
        properties.setProperty("K", "3");

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
        int k = Integer.parseInt(properties.getProperty("K"));

        return new Config(tickMs, maxTicks, deliveryProb, capacity, k);
    }
}