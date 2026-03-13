package warehouse;

import java.util.Map;

// We want to print in key=value format
public final class Printer {

    public synchronized void log(Map<String, Object> fields) {
        StringBuilder s = new StringBuilder();
        boolean first = true;

        for (var v : fields.entrySet()) {
            if (!first) {
                s.append(' ');
            }
            first = false;

            s.append(v.getKey()).append("=").append(v.getValue());
        }

        System.out.println(s.toString());
    }

    public synchronized void logKv(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("need key value pairs");
        }

        StringBuilder s = new StringBuilder();
        // prints key value pairs in a string
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) {
                s.append(" ");
            }

            s.append(kv[i]).append("=").append(kv[i + 1]);
        }

        System.out.println(s.toString());
    }
}