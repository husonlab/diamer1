package org.husonlab.diamer2.util.logging;

public class ProgressLogger extends LoggerElement {
    private final String itemName;
    private long processedItems;
    private long lastPrintTime;
    private long lastPrintItems;

    /**
     * Progress logger to print progress statistics for a task.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     */
    public ProgressLogger(String itemName) {
        this(itemName, 0);
    }

    /**
     * Progress logger to print progress statistics for a task.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     * @param processedItems Number of items that have already been processed.
     */
    public ProgressLogger(String itemName, long processedItems) {
        this.lastPrintItems = processedItems;
        this.itemName = itemName;
        this.processedItems = processedItems;
        this.lastPrintTime = System.currentTimeMillis();
    }

    /**
     * Logs the progress of the task.
     * Only prints the progress statistics if the time interval has passed.
     * @param processedItems Number of items that have already been processed.
     */
    public void setProgress(long processedItems) {
        this.processedItems = processedItems;
        logger.notifyProgress();
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
