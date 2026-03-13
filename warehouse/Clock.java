package warehouse;

public class Clock implements Runnable {// runnable as its gonna run inside a thread (clock will run indepenendently in
                                        // the background)
    // instead of using real time our warehouse runs on ticks
    private final long tickMs;// how many ms a tick should last
    private final long maxTicks;// how long clock should run before stopping

    private long tick = 0;

    private volatile boolean running = true;

    public Clock(long tickMs, long maxTicks) {
        if (tickMs < 1)
            throw new IllegalArgumentException("must be >= 1");
        if (maxTicks < 1)
            throw new IllegalArgumentException("must be >= 1");
        this.tickMs = tickMs;
        this.maxTicks = maxTicks;

    }

    public synchronized long current() {// current time
        return tick;
    }

    public synchronized void waitForTick(long goalTick) throws InterruptedException {// waiting until goal time is
                                                                                     // reached
        while (running && tick < goalTick) {
            wait();// thread sleeps while waiting for condition
        }
    }

    // example usage pause worker for 2 ticks - use in stocker thread
    public void sleepTicks(long ticks) throws InterruptedException {
        if (ticks < 0)
            throw new IllegalArgumentException("ticks must be >= 0");
        final long goal;
        synchronized (this) {
            goal = tick + ticks;
        }
        waitForTick(goal);
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void stop() {
        running = false;
        notifyAll();
    }

    @Override
    public void run() {
        try {
            while (running) {
                Thread.sleep(tickMs);// basically sets how long a tick is
                synchronized (this) {// after sleep move by one tick
                    tick++;
                    notifyAll();
                    if (tick >= maxTicks) {
                        running = false;
                        notifyAll();
                    }
                }
            }

        } catch (InterruptedException ie) {// if thread interrupted stop it
            stop();
            Thread.currentThread().interrupt();
        }
    }

}
