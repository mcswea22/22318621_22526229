package warehouse;

public final class Main {

    public static void main(String[] args) throws Exception {
        String configPath = (args.length >= 1) ? args[0] : "config.properties";// loads the config
        Config config = Config.load(configPath);
        // creating our system objects
        Printer printer = new Printer();
        Clock clock = new Clock(config.getTickMs(), config.getMaxTicks());
        Storage storage = new Storage();
        // sets up our pool of trolleys
        TrolleyManager trolleyManager = new TrolleyManager(
                config.getNumberOfTrolleys(),
                config.getTrolleyCapacity(),
                printer,
                clock);
        // creating our clock and delibery thread
        Thread clockThread = new Thread(clock, "CLOCK");
        Thread deliveryThread = new Thread(
                new DeliveryManager(clock, printer, storage, 12345L, config.getDeliveryProb()),
                "DEL");

        clockThread.start();
        deliveryThread.start();

        clockThread.join();

        deliveryThread.interrupt();
        deliveryThread.join();

        printer.logKv(
                "tick", clock.current(),
                "tid", "MAIN",
                "event", "simulation_end");
    }
}