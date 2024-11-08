package org.husonlab.diamer2.main;

public class Test {
    public static void main(String[] args) {
        TestParallel testParallel = new TestParallel(
                Integer.valueOf(args[0]),   // Threads
                Integer.valueOf(args[1]),   // max queue size
                Integer.valueOf(args[2]),   // fasta batch size
                new short[]{1, 10},
                args[3]);                   // path to fsa
        try {
            testParallel.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        testParallel.shutdown();
    }
}
