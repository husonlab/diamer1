package org.husonlab.diamer.util;

public class FlexibleIntArray {

    private int size;
    private int capacity;
    private int[] array;
    private static final int GROWTH_FACTOR = 2;

    public FlexibleIntArray(int initialCapacity) {
        this.size = 0;
        this.capacity = initialCapacity;
        this.array = new int[initialCapacity];
    }

    public void add(int value) {
        if (size == capacity) {
            grow();
        }
        array[size++] = value;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return array[index];
    }

    private void grow() {
        capacity *= GROWTH_FACTOR;
        int[] newArray = new int[capacity];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public int[] toArray() {
        int[] result = new int[size];
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
