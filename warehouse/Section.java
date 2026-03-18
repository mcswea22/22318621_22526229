package warehouse;

public class Section {
    private int boxes;
    private final int capacity;
    private boolean beingStocked = false;// tracks if stocker thread is currently adding boxes
    private int waitingPickers = 0;// counts how many picker threads are waiting

    public Section(int initial, int capacity) {
        this.boxes = initial;
        this.capacity = capacity;
    }

    public synchronized void takeBox() throws InterruptedException {// only one thread at a time can use this method
        waitingPickers++;// waiting to pick
        try {
            while (boxes == 0 || beingStocked) {
                wait();
            }
            boxes--;// take a box
            notifyAll();// notify waiting threads
        } finally {
            waitingPickers--;// one less waiting
        }
    }

    public synchronized void startStocking() throws InterruptedException {
        while (beingStocked) {// only one stocker can stock a section at a time
            wait();
        }
        beingStocked = true;// this section currently being stocked
    }

    public synchronized void stopStocking() {
        beingStocked = false;
        notifyAll();// let other threads know thar section is free
    }

    public synchronized void addBox() {
        if (boxes < capacity) {// add if not full
            boxes++;
            notifyAll();// let pickers know new box available and stockers that space changed
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
}