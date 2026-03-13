package warehouse;

import java.util.EnumMap;
import java.util.Random;

//this is where the boxes are kept before employees take them in
public class Storage {
    private final EnumMap<Type, Integer> counts = new EnumMap<>(Type.class);// how many of each type are in storage e.g.
                                                                            // 5 Electronics

    public Storage() {
        for (Type type : Type.values()) {
            counts.put(type, 0);
        }
    }

    public synchronized void add(Delivery delivery) {// adds a delivery into storage - synchronised so only one thread
                                                     // can access it at a time
        for (Type type : Type.values()) {// gets how many currently and adds new ones in
            counts.put(type, counts.get(type) + delivery.TypeCount(type));
        }
        notifyAll();// wakes any threads waiting on this
    }

    // wait until theres boxes and worker remove up to maxboxes randomly amd return
    // them as a delivery
    public synchronized Delivery takeRandom(int maxBoxes, Random r) throws InterruptedException {
        if (maxBoxes < 1) {
            throw new IllegalArgumentException("maxBoxes must be >= 1");
        }

        while (totalBoxes() == 0) {
            wait();
        }

        Delivery taken = new Delivery();// boxes removed from storage
        int boxesToTake = Math.min(maxBoxes, totalBoxes());// take max or less only

        for (int i = 0; i < boxesToTake; i++) {// randomly remove boxes
            Type chosenType = randomType(r);
            counts.put(chosenType, counts.get(chosenType) - 1);
            taken.DeliveryManager(chosenType, 1);
        }

        return taken;// object containing boxes that were removed
    }

    private int totalBoxes() {// count grand total of boxes
        int total = 0;
        for (Type type : Type.values()) {
            total += counts.get(type);
        }
        return total;
    }

    private Type randomType(Random r) {// chooses a random available box type
        while (true) {
            Type type = Type.values()[r.nextInt(Type.values().length)];
            if (counts.get(type) > 0) {
                return type;
            }
        }
    }
}