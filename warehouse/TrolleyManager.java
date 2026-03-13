package warehouse;

public class TrolleyManager {// class that manages shared pool of trolleys
    private final Trolley[] trolleys;// how many trolleys there are
    private final boolean[] inUse;// how many trolleys are in use

    private final Printer printer;
    private final Clock clock;

    public TrolleyManager(int n, int capacity, Printer printer, Clock clock) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1");// must be at least one trolley
        }

        this.printer = printer;
        this.clock = clock;

        this.trolleys = new Trolley[n];
        this.inUse = new boolean[n];

        for (int i = 0; i < n; i++) {// setting trolleys initiallu as free and setting their IDS
            trolleys[i] = new Trolley(i + 1, capacity);
            inUse[i] = false;
        }
    }

    // method to give worker a trolley, if none available then they wait
    public synchronized Trolley acquire(String tid) throws InterruptedException {
        long start = clock.current();// says when worker started waiting to get a trollet

        while (true) {
            for (int i = 0; i < trolleys.length; i++) {
                if (!inUse[i]) {
                    inUse[i] = true;

                    long waited = clock.current() - start;// how long the worker waited for trolley
                    Trolley trolley = trolleys[i];// assigning them the free one

                    printer.logKv(
                            "tick", clock.current(),
                            "tid", tid,
                            "event", "acquire_trolley",
                            "trolley_id", trolley.getTrolleyID(),
                            "waited_ticks", waited);

                    return trolley;
                }
            }

            wait();// if no trolleys were free wait
        }
    }

    public synchronized void release(String tid, Trolley trolley) {
        if (trolley.totalLoad() > 0) {// cant release a trolley that still has boxes
            throw new IllegalStateException(
                    "Cannot release trolley " + trolley.getTrolleyID() + " while load > 0");
        }

        for (int i = 0; i < trolleys.length; i++) {// finding trolley that needs to be returned
            if (trolleys[i] == trolley) {
                inUse[i] = false;// free it up
                notifyAll();// let threads waiting on it know

                printer.logKv(// log it
                        "tick", clock.current(),
                        "tid", tid,
                        "event", "release_trolley",
                        "trolley_id", trolley.getTrolleyID(),
                        "waited_ticks", 0);

                return;
            }
        }

        throw new IllegalArgumentException("Unknown trolley released");// exta error handling
    }
}