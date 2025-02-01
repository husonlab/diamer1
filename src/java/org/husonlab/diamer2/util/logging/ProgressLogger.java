package org.husonlab.diamer2.util.logging;

import org.jetbrains.annotations.NotNull;

/**
 * {@link LoggerElement} to log the progress of a task in the form of processed items in total and per second.
 */
public class ProgressLogger extends LoggerElement {
    private final String itemName;
    private long processedItems;
    private long lastPrintTime;
    private long lastPrintItems;

    /**
     * Constructs a {@link ProgressLogger} with the initial number of processed items set to 0.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     */
    public ProgressLogger(@NotNull String itemName) {
        this(itemName, 0);
    }

    /**
     * Constructs a {@link ProgressLogger} with the initial number of processed items set to the given value.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     * @param processedItems Number of items that have already been processed.
     */
    public ProgressLogger(String itemName, long processedItems) {
        lastPrintItems = processedItems;
        this.itemName = itemName;
        this.processedItems = processedItems;
        lastPrintTime = System.currentTimeMillis();
    }

    /**
     * Logs the progress of the task.
     * Only prints the progress statistics if the time interval has passed.
     * @param processedItems Number of items that have already been processed.
     */
    public void setProgress(long processedItems) {
        this.processedItems = processedItems;
        logger.notifyUpdate();
    }

    /**
     * Increments the number of processed items by one.
     */
    public void incrementProgress() {
        processedItems++;
        logger.notifyUpdate();
    }

    @Override
    String getMessage() {
        long currentTime = System.currentTimeMillis();
        long intervalItems = processedItems - lastPrintItems;
        lastPrintItems = processedItems;
        float itemsPerSecond = (float) intervalItems / (currentTime - lastPrintTime) * 1000;
        lastPrintTime = currentTime;
        return " %.2fM %s processed (%.2f %s/s) ".formatted(processedItems*1e-6, itemName, itemsPerSecond, itemName);
    }
}
