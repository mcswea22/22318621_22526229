package warehouse;

public class TrolleyManager {// class that manages shared pool of trolleys
    private final Trolley[] trolleys;// how many trolleys there are
    private final boolean[] inUse;// how many trolleys are in use

    private final Printer printer;
    private final Clock clock;

    private long nextTicket = 0; // next ticket number to give out
    private long serving = 0; // ticket number currently being served

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
        long start = clock.current(); // when this worker started waiting
        long myTicket = nextTicket++; // give this worker a ticket

        while (true) {
            // only the worker whose turn it is can try to take a trolley
            if (myTicket == serving) {
                for (int i = 0; i < trolleys.length; i++) {
                    if (!inUse[i]) {
                        inUse[i] = true;

                        long waited = clock.current() - start;
                        Trolley trolley = trolleys[i];

                        serving++; // let the next ticket holder have their turn
                        notifyAll();

                        printer.logKv(
                                "tick", clock.current(),
                                "tid", tid,
                                "event", "acquire_trolley",
                                "trolley_id", trolley.getTrolleyID(),
                                "waited_ticks", waited);

                        return trolley;
                    }
                }
            }

            wait(); // wait until a trolley is released or it becomes this thread's turn
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