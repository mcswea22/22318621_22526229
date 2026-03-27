package warehouse;

public class TrolleyManager { // class that manages shared pool of trolleys
    private final Trolley[] trolleys; // how many trolleys there are
    private final boolean[] inUse; // which trolleys are in use

    private final Printer printer;
    private final Clock clock;
    private final Storage storage;

    private long nextTicket = 0; // next ticket number to give out
    private long serving = 0;    // ticket number currently being served

    public TrolleyManager(int n, int capacity, Printer printer, Clock clock, Storage storage) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1"); // must be at least one trolley
        }

        this.printer = printer;
        this.clock = clock;
        this.storage = storage; // need access to storage to check staging items

        this.trolleys = new Trolley[n];
        this.inUse = new boolean[n];

        for (int i = 0; i < n; i++) { // set trolleys initially as free and assign IDs
            trolleys[i] = new Trolley(i + 1, capacity);
            inUse[i] = false;
        }
    }

    // method to give worker a trolley, if none available then they wait
    public synchronized Trolley acquire(String tid, boolean isPicker) throws InterruptedException {
        long start = clock.current();   // when this worker started waiting
        long myTicket = nextTicket++;   // give this worker a ticket

        while (true) {
            // enforce ticket order
            while (myTicket != serving) {
                wait();
            }

            // count free trolleys
            int freeCount = 0;
            for (boolean used : inUse) {
                if (!used) {
                    freeCount++;
                }
            }

            // do not let a picker take the last free trolley if there is stock in staging
            if (isPicker && freeCount == 1 && storage.hasItems()) {
                wait();
                continue;
            }

            for (int i = 0; i < trolleys.length; i++) {
                if (!inUse[i]) {
                    inUse[i] = true;

                    long waited = clock.current() - start;
                    Trolley trolley = trolleys[i];

                    serving++;   // let the next ticket holder have their turn
                    notifyAll();

                    printer.logKv(
                        "tick", clock.current(),
                        "tid", tid,
                        "event", "acquire_trolley",
                        "trolley_id", trolley.getTrolleyID(),
                        "waited_ticks", waited
                    );

                    return trolley;
                }
            }

            wait(); // wait until a trolley is released
        }
    }

    public synchronized void release(String tid, Trolley trolley) {
        if (trolley.totalLoad() > 0) { // cant release a trolley that still has boxes
            throw new IllegalStateException(
                "Cannot release trolley " + trolley.getTrolleyID() + " while load > 0");
        }

        for (int i = 0; i < trolleys.length; i++) { // find trolley that needs to be returned
            if (trolleys[i] == trolley) {
                inUse[i] = false; // free it up
                notifyAll();      // let waiting threads know

                printer.logKv(
                    "tick", clock.current(),
                    "tid", tid,
                    "event", "release_trolley",
                    "trolley_id", trolley.getTrolleyID(),
                    "remaining_load", trolley.totalLoad()
                );

                return;
            }
        }

        throw new IllegalArgumentException("Unknown trolley released");
    }
}