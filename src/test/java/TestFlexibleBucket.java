import org.husonlab.diamer2.indexing.Sorting;
import org.husonlab.diamer2.util.FlexibleBucket;
import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

public class TestFlexibleBucket {
    @Test
    public void testFlexibleBucket() {
        FlexibleBucket fb = new FlexibleBucket(5, 5, 3);
        int contingentLimit = fb.getContingent().last();
        for (int i = 0; i < 100; i++) {
            if (i >= contingentLimit) {
                contingentLimit = fb.getContingent().last();
            }
            fb.set(i, ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextInt());
        }
        ForkJoinPool pool = new ForkJoinPool();
        pool.submit(new Sorting.MsdRadixTaskFlexibleBucket(fb)).join();
        System.out.println("Size: " + fb.size());
    }
}
