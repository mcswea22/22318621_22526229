package warehouse;

public class Section {
    private int boxes;
    private final int capacity;
    private boolean beingStocked = false; // tracks if stocker thread is currently adding boxes
    private int waitingPickers = 0; // counts how many picker threads are waiting
    private int activePickers = 0; // counts how many picker threads are currently picking

    public Section(int initial, int capacity) {
        this.boxes = initial;
        this.capacity = capacity;
    }

    // reserve one box and mark this picker as active
    public synchronized void beginPick() throws InterruptedException {
        waitingPickers++;
        try {
            while (boxes == 0 || beingStocked) {
                wait();
            }

            boxes--;         // reserve/remove one box immediately
            activePickers++; // mark picker in progress
        } finally {
            waitingPickers--;
        }
    }

    // finish the pick and notify waiting threads
    public synchronized void endPick() {
        if (activePickers <= 0) {
            throw new IllegalStateException("endPick called with no active picker");
        }
        activePickers--;
        notifyAll();
    }

    public synchronized void startStocking() throws InterruptedException {
        while (
            beingStocked ||                   // another stocker already using section
            isFull() ||                       // section already full
            activePickers > 0 ||              // don't stock while picker is actively picking
            (waitingPickers > 0 && boxes > 0) // let waiting pickers take available boxes first
        ) {
            wait();
        }
        beingStocked = true;
    }

    public synchronized void stopStocking() {
        beingStocked = false;
        notifyAll();
    }

    public synchronized void addBox() {
        if (boxes < capacity) {
            boxes++;
            notifyAll();
        }
    }

    public synchronized int getBoxes() {
        return boxes;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized boolean isBeingStocked() {
        return beingStocked;
    }

    public synchronized boolean isFull() {
        return boxes >= capacity;
    }

    public synchronized int getWaitingPickers() {
        return waitingPickers;
    }

    public synchronized int getActivePickers() {
        return activePickers;
    }
}