import org.husonlab.diamer2.indexing.Sorting;
import org.husonlab.diamer2.util.FlexibleDBBucket;
import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

public class TestFlexibleDBBucket {
    @Test
    public void testFlexibleBucket() {
        FlexibleDBBucket fb = new FlexibleDBBucket(5, 5, 3);
        int contingentLimit = fb.getContingent().last();
        for (int i = 0; i < 100; i++) {
            if (i >= contingentLimit) {
                contingentLimit = fb.getContingent().last();
            }
            fb.set(i, ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextInt());
        }
        ForkJoinPool pool = new ForkJoinPool();
        pool.submit(new Sorting.MsdRadixTaskFlexibleDBBucket(fb)).join();
        System.out.println("Size: " + fb.size());
    }
}
