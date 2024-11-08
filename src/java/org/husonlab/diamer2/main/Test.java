package org.husonlab.diamer2.main;

public class Test {
    public static void main(String[] args) {
        TestParallel testParallel = new TestParallel(
                16,
                1,
                100,
                new short[]{1, 10},
                "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\nr100.fsa");
        try {
            testParallel.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        testParallel.shutdown();
    }
}
