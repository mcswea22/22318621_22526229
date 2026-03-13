package warehouse;

import java.util.EnumMap;

public class Trolley {// class for a single trolley

    private final int trolleyID;
    private final int capacity;// max no of boxes a trolley can hold
    private final EnumMap<Type, Integer> load = new EnumMap<>(Type.class);// hpw many boxes of each type are currently
                                                                          // on the trolley
    private int totalLoad = 0;// total no of boxes on the trolley

    public Trolley(int trolleyID, int capacity) {
        this.trolleyID = trolleyID;
        this.capacity = capacity;
        for (Type t : Type.values()) {// set every type as 0 initially
            load.put(t, 0);
        }
    }

    public int getTrolleyID() {// get trolley ID
        return trolleyID;
    }

    public int getCapacity() {// get max no of boxes trolley can hold
        return capacity;
    }

    public synchronized int totalLoad() {// get total load of ttolley
        return totalLoad;
    }

    public synchronized int get(Type type) {// get types on the trolley
        return load.get(type);
    }

    public synchronized void clear() {// removes all boxes from the trolley
        for (Type t : Type.values()) {
            load.put(t, 0);
        }
        totalLoad = 0;
    }

    public synchronized void add(Type type, int amount) {// adding boxes of certain type to the trolley
        load.put(type, load.get(type) + amount);
        totalLoad += amount;
    }

    public synchronized EnumMap<Type, Integer> loadCopy() {
        return new EnumMap<>(load);// returns copy of whats on the trolley
    }
}
