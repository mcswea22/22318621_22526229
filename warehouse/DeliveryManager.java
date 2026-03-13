package warehouse;

import java.util.Random;

public final class DeliveryManager implements Runnable {
    // this class generates our deliveries and puts them into storage
    private final Clock clock;
    private final Printer printer;
    private final Storage storage;
    private final Random random;
    private final double deliveryProb;// probabilty delivery arrives on each tick
    private final String tid = "DEL";

    public DeliveryManager(Clock clock, Printer printer, Storage storage,
            long seed, double deliveryProb) {
        this.clock = clock;
        this.printer = printer;
        this.storage = storage;
        this.random = new Random(seed);
        this.deliveryProb = deliveryProb;
    }

    @Override
    public void run() {
        try {
            long currentTick = clock.current();

            while (clock.isRunning()) {
                // wait for the next tick (time to progress)
                clock.waitForTick(currentTick + 1);
                currentTick++;

                if (random.nextDouble() < deliveryProb) {// decides whether a delivery happens on this tick or not
                    Delivery delivery = createRandomDelivery();
                    storage.add(delivery);

                    printer.logKv(
                            "tick", clock.current(),
                            "tid", tid,
                            "event", "delivery_arrived",
                            "electronics", delivery.TypeCount(Type.ELECTRONICS),
                            "books", delivery.TypeCount(Type.BOOKS),
                            "medicines", delivery.TypeCount(Type.MEDICINES),
                            "clothes", delivery.TypeCount(Type.CLOTHES),
                            "tools", delivery.TypeCount(Type.TOOLS));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Delivery createRandomDelivery() {
        Delivery delivery = new Delivery();

        for (int i = 0; i < 10; i++) {// 10 boxes of random items
            Type type = Type.values()[random.nextInt(Type.values().length)];
            delivery.DeliveryManager(type, 1);
        }

        return delivery;
    }
}