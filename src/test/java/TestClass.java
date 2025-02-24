import org.junit.Test;

import static org.husonlab.diamer2.seq.converter.Utilities.generateToAAAndBase11AndNumberFR;

public class TestClass {
    @Test
    public void test() {
        generateToAAAndBase11AndNumberFR();
    }

    @Test
    public void test2() {
        try {
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            System.out.println("Catch block");
        } finally {
            System.out.println("Finally block");
        }
    }
}
