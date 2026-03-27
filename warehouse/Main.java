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
            clock,
            storage);
        Warehouse warehouse = new Warehouse(config.getSectionCapacity());
        ID idSource = new ID();
        // mean time between pick attempts is 10 ticks per picker
        // so more pickers = more time between attempts on average
        int meanPickerAttemptGapTicks = 10 * config.getNumPickers();
        // creating our pickers;
        int numPickers = config.getNumPickers();
        Thread[] pickers = new Thread[numPickers];

        for (int i = 1; i <= numPickers; i++) {
            pickers[i - 1] = new Picker(
                    "P" + i,
                    warehouse,
                    clock,
                    printer,
                    trolleyManager,
                    idSource,
                    1000L * i + 7,
                    meanPickerAttemptGapTicks
            );
        }

        // creating our clock and delibery thread
        Thread clockThread = new Thread(clock, "CLOCK");
        Thread deliveryThread = new Thread(
                new DeliveryManager(clock, printer, storage, 12345L, config.getDeliveryProb()),
                "DEL");
        // creating our stockers
        int numStockers = config.getNumStockers();
        Thread[] stockers = new Thread[numStockers];
        // each stocker has a different seed for randomness
        for (int i = 1; i <= numStockers; i++) {
            stockers[i - 1] = new Stocker(
                    "S" + i,
                    warehouse,
                    clock,
                    printer,
                    storage,
                    trolleyManager,
                    99L + i
            );
        }

        clockThread.start();
        deliveryThread.start();

        for (Thread stocker : stockers) {
            stocker.start();
        }

        for (Thread picker : pickers) {
            picker.start();
        }

        clockThread.join();

        for (Thread picker : pickers) {
            picker.interrupt();
        }

        for (Thread picker : pickers) {
            picker.join();
        }

        for (Thread stocker : stockers) {
            stocker.interrupt();
        }

        for (Thread stocker : stockers) {
            stocker.join();
        }

        deliveryThread.interrupt();
        deliveryThread.join();

        printer.logKv(
                "tick", clock.current(),
                "tid", "MAIN",
                "event", "simulation_end");
    }
}