package warehouse;

import java.util.EnumMap;

public class Delivery {
    private final EnumMap<Type, Integer> counts = new EnumMap<>(Type.class);// our items mapped outs

    public Delivery() {
        for (Type t : Type.values())
            counts.put(t, 0);// set initial delivery as empty
    }

    public void IncomingDelivery(Type t, int n) {// adding deliveries to current stock
        counts.put(t, counts.get(t) + n);
    }

    public int TypeCount(Type t) {// count of indivudally grouped item e.g. electronics
        return counts.get(t);
    }

    public int StockCount() {// count of overall stock
        int total = 0;
        for (Type t : Type.values())
            total += counts.get(t);
        return total;
    }

    public EnumMap<Type, Integer> asMap() {// this is to copy the map so original map cant be altered
        return new EnumMap<>(counts);
    }
}
