//https://hemanthcse1.medium.com/atomiclong-63b433b9dfea
//AtomicLong is required for threads
package warehouse;

import java.util.concurrent.atomic.AtomicLong;

public class ID {
    private final AtomicLong pickID = new AtomicLong(1);// the counter

    public long newID() {
        return pickID.getAndAdd(1);// incrementing our ID
    }

}
