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
        synchronized (this) {
            this.processedItems = processedItems;
        }
        logger.notifyUpdate();
    }

    /**
     * Logs the progress of the task without notifying the logger.
     * @param processedItems Number of items that have already been processed.
     */
    public void setProgressSilent(long processedItems) {
        this.processedItems = processedItems;
    }

    /**
     * Increments the number of processed items by one.
     */
    public void incrementProgress() {
        synchronized (this) {
            processedItems++;
        }
        logger.notifyUpdate();
    }

    /**
     * Increments the number of processed items by a given amount.
     * @param amount Number of items to increment the progress by.
     */
    public void incrementProgress(long amount) {
        synchronized (this) {
            processedItems += amount;
        }
        logger.notifyUpdate();
    }

    @Override
    String getMessage() {
        long currentTime = System.currentTimeMillis();
        long intervalItems = processedItems - lastPrintItems;
        lastPrintItems = processedItems;
        double itemsPerSecond = (double) intervalItems / (currentTime - lastPrintTime) * 1000.0;
        lastPrintTime = currentTime;
        return " %.2fM %s processed (%.2f %s/s) ".formatted(processedItems*1e-6, itemName, itemsPerSecond, itemName);
    }
}
