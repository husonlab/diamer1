package org.husonlab.diamer.util;

/**
 * Implementation of a flexible array for longs.
 * Better than an ArrayList because it does store the values as primitives.
 */
public class FlexibleLongArray {

    private int size;
    private int capacity;
    private long[] array;
    private static final int GROWTH_FACTOR = 2;

    public FlexibleLongArray(int initialCapacity) {
        this.size = 0;
        this.capacity = initialCapacity;
        this.array = new long[initialCapacity];
    }

    public void add(long value) {
        if (size == capacity) {
            grow();
        }
        array[size++] = value;
    }

    public long get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return array[index];
    }

    private void grow() {
        capacity *= GROWTH_FACTOR;
        long[] newArray = new long[capacity];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public long[] toArray() {
        long[] result = new long[size];
        System.arraycopy(array, 0, result, 0, size);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < size; i++) {
            sb.append(array[i]).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }
}
