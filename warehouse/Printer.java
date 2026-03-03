package warehouse;

import java.util.Map;

//we want to print out in key=value format
public final class Printer {
    public synchronized void log(Map<String, Object> fields) {// synchronised so its only one thread at a time
        StringBuilder s = new StringBuilder();
        for (var v : fields.entrySet()) {
            boolean first = true;// we dont want space at the start so we set this flag
            if (!first)
                s.append(' ');// adding space in between
            first = false;
            s.append(v.getKey()).append("=").append(v.getValue());// key=val format
        }
        System.out.println(s.toString());
    }

}
