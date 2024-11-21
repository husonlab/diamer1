package org.husonlab.diamer2.logging;

public class ProgressLogger {
    private final String itemName;
    private final String prefix;
    private long processedItems;
    private final long startTime;
    private final long timeInterval;
    private long lastPrintTime;

    /**
     * Progress logger to print progress statistics for a task.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     * @param prefix Prefix to print before the progress statistics (e.g. [Indexer]).
     * @param timeInterval Time interval in milliseconds to print the progress statistics.
     */
    public ProgressLogger(String itemName, String prefix, long timeInterval) {
        this.itemName = itemName;
        this.prefix = prefix;
        this.processedItems = 0L;
        this.startTime = System.currentTimeMillis();
        this.timeInterval = timeInterval;
        this.lastPrintTime = startTime;
    }

    /**
     * Progress logger to print progress statistics for a task.
     * @param itemName Name of the item that is processed (e.g. reads, fastas).
     * @param prefix Prefix to print before the progress statistics (e.g. [Indexer]).
     * @param timeInterval Time interval in milliseconds to print the progress statistics.
     * @param processedItems Number of items that have already been processed.
     */
    public ProgressLogger(String itemName, String prefix, long timeInterval, long processedItems) {
        this.itemName = itemName;
        this.prefix = prefix;
        this.processedItems = processedItems;
        this.startTime = System.currentTimeMillis();
        this.timeInterval = timeInterval;
        this.lastPrintTime = startTime;
    }

    /**
     * Logs the progress of the task.
     * Only prints the progress statistics if the time interval has passed.
     * @param processedItems Number of items that have already been processed.
     */
    public void logProgress(long processedItems) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPrintTime > timeInterval) {
            long itemsProcessed = processedItems - this.processedItems;
            this.processedItems = processedItems;
            float itemsPerSecond = (float) itemsProcessed / (currentTime - lastPrintTime) * 1000;
            System.out.printf("%s %.2fM %s processed (%.2f %s/s)\n",
                    prefix, processedItems*1e-6, itemName, itemsPerSecond, itemName);
            lastPrintTime = currentTime;
        }
    }

}
