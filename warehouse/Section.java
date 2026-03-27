package warehouse;

public class Section {
    private int boxes;
    private final int capacity;
    private boolean beingStocked = false; // tracks if stocker thread is currently adding boxes
    private int waitingPickers = 0; // counts how many picker threads are waiting

    public Section(int initial, int capacity) {
        this.boxes = initial;
        this.capacity = capacity;
    }

    public synchronized void takeBox() throws InterruptedException {
        while (boxes == 0 || beingStocked) {
            waitingPickers++;
            try {
                wait();
            } finally {
                waitingPickers--;
            }
        }
    }

    public synchronized void startStocking() throws InterruptedException {
        while (beingStocked || // another stocker already using section
                isFull() || // section is already full so doesnt need to be stocked
                (waitingPickers > 0 && boxes > 0) // if pickers are waiting let them pick before stocking
        ) {
            wait();
        }
        beingStocked = true;// this section is currently being stocked
    }

    public synchronized void stopStocking() {
        beingStocked = false;
        notifyAll();// let other threads know section is free
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