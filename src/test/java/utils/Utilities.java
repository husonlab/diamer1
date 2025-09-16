package utils;

public class Utilities {
    public static <T> void assertIsIn(T element, Iterable<T> collection) {
        for (T item : collection) {
            if (item.equals(element)) {
                return;
            }
        }
        throw new AssertionError("Element " + element + " not found in collection.");
    }
}
