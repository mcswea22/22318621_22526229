package warehouse;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

// This class represents a stocker worker (runs as a thread)
public class Stocker extends Thread {

    private final String tid; // ID of this stocker thread
    private final Warehouse warehouse; // warehouse with sections
    private final Clock clock; // simulation clock
    private final Printer printer; // used for logging events
    private final Storage storage; // where items are taken from
    private final TrolleyManager trolleyManager; // manages trolley usage
    private final Random random; // random for decisions (breaks, loads)

    // next time (tick) when the stocker will take a break
    private long nextBreakTick;

    // constructor
    public Stocker(String tid,
            Warehouse warehouse,
            Clock clock,
            Printer printer,
            Storage storage,
            TrolleyManager trolleyManager,
            long seed) {

        super(tid);
        this.tid = tid;
        this.warehouse = warehouse;
        this.clock = clock;
        this.printer = printer;
        this.storage = storage;
        this.trolleyManager = trolleyManager;
        this.random = new Random(seed);

        // first break happens between 200–300 ticks
        this.nextBreakTick = 200 + random.nextInt(101);
    }

    @Override
    public void run() {
        try {
            // main loop runs while simulation is active
            while (!isInterrupted() && clock.isRunning()) {

                maybeTakeBreakAtStaging(); // check if it's time for a break

                // wait until storage has items
                storage.waitForItems();

                // get a trolley (may block if none available)
                Trolley trolley = trolleyManager.acquire(tid, false);

                boolean released = false; // track if trolley was released

                try {
                    // take random boxes from storage
                    Delivery delivery = storage.takeRandom(trolley.getCapacity(), random);
                    EnumMap<Type, Integer> remaining = delivery.asMap();

                    boolean madeProgressThisLoad = false; // track if we stocked anything

                    int total = totalBoxes(remaining);

                    // if nothing was taken, release trolley and retry later
                    if (total == 0) {
                        trolleyManager.release(tid, trolley);
                        released = true;
                        clock.sleepTicks(5);
                        continue;
                    }

                    // loading takes 1 tick
                    clock.sleepTicks(1);

                    // update trolley contents to match what we took
                    syncTrolleyLoad(trolley, remaining);

                    // log load event
                    printer.logKv(
                            "tick", clock.current(),
                            "tid", tid,
                            "event", "stocker_load",
                            "electronics", remaining.getOrDefault(Type.ELECTRONICS, 0),
                            "books", remaining.getOrDefault(Type.BOOKS, 0),
                            "medicines", remaining.getOrDefault(Type.MEDICINES, 0),
                            "clothes", remaining.getOrDefault(Type.CLOTHES, 0),
                            "tools", remaining.getOrDefault(Type.TOOLS, 0),
                            "total_load", trolley.totalLoad());

                    String currentLocation = "staging"; // start at staging

                    // keep stocking while trolley has items
                    while (trolley.totalLoad() > 0 && clock.isRunning() && !isInterrupted()) {

                        // decide which section to go to next
                        Type target = chooseNextSection(remaining);
                        if (target == null) {
                            clock.sleepTicks(5);
                            continue;
                        }

                        int intended = remaining.getOrDefault(target, 0);
                        if (intended <= 0) {
                            continue;
                        }

                        // move to the section if not already there
                        int moveLoad = currentLocation.equals("staging") ? intended : trolley.totalLoad();
                        move(currentLocation, target.name().toLowerCase(), moveLoad, trolley);
                        currentLocation = target.name().toLowerCase();

                        Section section = warehouse.getSection(target);

                        section.startStocking(); // lock section for stocking
                        try {
                            // log start of stocking
                            printer.logKv(
                                    "tick", clock.current(),
                                    "tid", tid,
                                    "event", "stock_begin",
                                    "section", target.name().toLowerCase(),
                                    "amount", intended,
                                    "trolley_id", trolley.getTrolleyID());

                            int stocked = 0;

                            // add boxes until done or section is full
                            while (remaining.get(target) > 0 && !section.isFull()) {
                                section.addBox();
                                clock.sleepTicks(1); // each box takes 1 tick
                                stocked++;
                                remaining.put(target, remaining.get(target) - 1);

                                syncTrolleyLoad(trolley, remaining); // update trolley
                            }

                            // mark that we actually stocked something
                            if (stocked > 0) {
                                madeProgressThisLoad = true;
                            }

                            // log end of stocking
                            printer.logKv(
                                    "tick", clock.current(),
                                    "tid", tid,
                                    "event", "stock_end",
                                    "section", target.name().toLowerCase(),
                                    "stocked", stocked,
                                    "remaining_load", trolley.totalLoad(),
                                    "trolley_id", trolley.getTrolleyID());

                        } finally {
                            section.stopStocking(); // unlock section
                        }
                    }

                    // go back to staging after finishing
                    if (!currentLocation.equals("staging")) {
                        move(currentLocation, "staging", trolley.totalLoad(), trolley);
                        currentLocation = "staging";
                    }

                    // if trolley is empty, release it
                    if (trolley.totalLoad() == 0) {
                        trolleyManager.release(tid, trolley);
                        released = true;

                        // rest depending on whether we did useful work
                        if (madeProgressThisLoad) {
                            clock.sleepTicks(1);
                        } else {
                            clock.sleepTicks(10);
                        }
                    }

                } finally {
                    // safety: make sure trolley is released if needed
                    if (!released) {
                        try {
                            if (trolley.totalLoad() == 0) {
                                trolleyManager.release(tid, trolley);
                            }
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            interrupt(); // re-interrupt thread
        }
    }

    // handles stocker breaks
    private void maybeTakeBreakAtStaging() throws InterruptedException {
        if (clock.current() < nextBreakTick) {
            return;
        }

        // log break start
        printer.logKv(
                "tick", clock.current(),
                "tid", tid,
                "event", "stocker_break_start");

        clock.sleepTicks(150); // break duration

        // log break end
        printer.logKv(
                "tick", clock.current(),
                "tid", tid,
                "event", "stocker_break_end");

        // schedule next break
        nextBreakTick = clock.current() + 200 + random.nextInt(101);
    }

    // count total boxes in the load
    private int totalBoxes(EnumMap<Type, Integer> remaining) {
        int total = 0;
        for (int value : remaining.values()) {
            total += value;
        }
        return total;
    }

    // decide best section to stock next
    private Type chooseNextSection(EnumMap<Type, Integer> remaining) {
        Type best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Type t : Type.values()) {
            int count = remaining.getOrDefault(t, 0);
            if (count <= 0)
                continue;

            Section s = warehouse.getSection(t);
            if (s.isFull())
                continue;

            int score = 0;

            // priority if section is empty
            if (s.getBoxes() == 0) {
                score += 1000;
            }

            // more pickers waiting = higher priority
            score += s.getWaitingPickers() * 100;

            // more boxes = higher priority
            score += count * 10;

            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }

        return best;
    }

    // simulate movement between locations
    private void move(String from, String to, int carriedLoad, Trolley trolley) throws InterruptedException {
        int ticks = 10 + Math.max(0, carriedLoad);

        if (carriedLoad == 0) {
            ticks = 10; // minimum travel time
        }

        clock.sleepTicks(ticks);

        // log movement
        printer.logKv(
                "tick", clock.current(),
                "tid", tid,
                "event", "move",
                "from", from,
                "to", to,
                "load", carriedLoad,
                "trolley_id", trolley.getTrolleyID());
    }

    // update trolley contents to match remaining boxes
    private void syncTrolleyLoad(Trolley trolley, Map<Type, Integer> remaining) {
        trolley.clear();

        for (Type type : Type.values()) {
            int count = remaining.getOrDefault(type, 0);
            if (count > 0) {
                trolley.add(type, count);
            }
        }
    }
}