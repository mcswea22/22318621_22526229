package warehouse;

import java.util.Random;

// This class represents a picker worker (runs as a thread)
// Pickers take items from sections using a trolley
public class Picker extends Thread {

    private final String tid; // thread ID
    private final Warehouse warehouse; // warehouse with all sections
    private final Clock clock; // simulation clock
    private final Printer printer; // used for logging events
    private final TrolleyManager trolleyManager; // manages trolley usage
    private final ID idSource; // generates unique pick IDs
    private final Random random; // random for choosing sections + delays
    private final int meanAttemptGapTicks; // average delay between picks

    // constructor
    public Picker(String tid,
            Warehouse warehouse,
            Clock clock,
            Printer printer,
            TrolleyManager trolleyManager,
            ID idSource,
            long seed,
            int meanAttemptGapTicks) {

        super(tid);
        this.tid = tid;
        this.warehouse = warehouse;
        this.clock = clock;
        this.printer = printer;
        this.trolleyManager = trolleyManager;
        this.idSource = idSource;
        this.random = new Random(seed);

        // make sure delay is at least 1 tick
        this.meanAttemptGapTicks = Math.max(1, meanAttemptGapTicks);
    }

    @Override
    public void run() {
        try {
            // main loop runs while simulation is active
            while (!isInterrupted() && clock.isRunning()) {

                // get a trolley (pickers may have priority depending on system)
                Trolley trolley = trolleyManager.acquire(tid, true);

                boolean released = false; // track if trolley is released
                boolean pickedStarted = false; // track if pick started
                Section section = null;

                try {
                    // generate a unique ID for this pick
                    long pickId = idSource.newID();

                    // randomly choose a section to pick from
                    Type chosen = chooseSectionOnce();
                    section = warehouse.getSection(chosen);

                    // log start of picking
                    printer.logKv(
                            "tick", clock.current(),
                            "tid", tid,
                            "event", "pick_start",
                            "pick_id", pickId,
                            "section", chosen.name().toLowerCase(),
                            "trolley_id", trolley.getTrolleyID());

                    // record how long we wait to access the section
                    long waitStart = clock.current();

                    section.beginPick(); // try to access section
                    pickedStarted = true;

                    long waitEnd = clock.current();
                    long waitedTicks = waitEnd - waitStart;

                    try {
                        // picking one item takes 1 tick
                        clock.sleepTicks(1);
                    } finally {
                        section.endPick(); // release section
                        pickedStarted = false;
                    }

                    // log end of picking
                    printer.logKv(
                            "tick", clock.current(),
                            "tid", tid,
                            "event", "pick_done",
                            "pick_id", pickId,
                            "section", chosen.name().toLowerCase(),
                            "waited_ticks", waitedTicks,
                            "trolley_id", trolley.getTrolleyID());

                    // release trolley after done
                    trolleyManager.release(tid, trolley);
                    released = true;

                    // wait before next pick attempt
                    pauseBeforeNextAttempt();

                } finally {
                    // safety: if something went wrong, make sure section is released
                    if (pickedStarted && section != null) {
                        try {
                            section.endPick();
                        } catch (RuntimeException ignored) {
                        }
                    }

                    // safety: make sure trolley is released
                    if (!released) {
                        try {
                            trolleyManager.release(tid, trolley);
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            interrupt(); // re-interrupt thread
        }
    }

    // randomly pick a section type
    private Type chooseSectionOnce() {
        Type[] all = Type.values();
        return all[random.nextInt(all.length)];
    }

    // wait a random time before next pick attempt
    private void pauseBeforeNextAttempt() throws InterruptedException {

        // delay range: half to 1.5x of mean
        int minDelay = Math.max(1, meanAttemptGapTicks / 2);
        int maxDelay = Math.max(minDelay, (meanAttemptGapTicks * 3) / 2);

        int delay = minDelay + random.nextInt(maxDelay - minDelay + 1);

        clock.sleepTicks(delay);
    }
}