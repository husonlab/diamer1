package org.husonlab.diamer.util.logging;

/**
 * {@link LoggerElement} that displays a progress bar of a given length.
 */
public class ProgressBar extends LoggerElement {

    private long total;
    private long progress;
    private final int length;

    /**
     * Create a new progress bar.
     * @param total The total number of steps to be completed.
     * @param length The length of the progress bar in characters.
     */
    public ProgressBar(long total, int length) {
        this.total = total;
        this.length = length;
    }

    @Override
    public String getMessage() {
        if (total > 0) {
            double percent = (100.0 * progress / total);
            int bars = (int) percent * length / 100;
            return "[" +
                    "=".repeat(Math.max(0, bars)) + " ".repeat(Math.max(0, length - bars)) +
                    "] " + "%.2f".formatted(percent) + "%";
        }
        return "";
    }

    /**
     * Set the total number of steps to be completed.
     * @param total The total number of steps to be completed.
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * Increment the progress by one step.
     */
    public void incrementProgress() {
        synchronized (this) {
            progress = Math.min(progress + 1, total);
        }
        logger.notifyUpdate();
    }

    /**
     * Increment the progress by a given amount.
     * @param amount The number of steps to increment the progress by.
     */
    public void incrementProgress(int amount) {
        synchronized (this) {
            progress = Math.min(progress + amount, total);
        }
        logger.notifyUpdate();
    }

    /**
     * Set the progress (total number of steps completed).
     * @param progress The total number of steps that have been completed.
     */
    public void setProgress(long progress) {
        synchronized (this) {
            this.progress = Math.min(progress, total);
        }
        logger.notifyUpdate();
    }

    /**
     * Set the progress (total number of steps completed) without notifying the logger.
     * @param progress The total number of steps that have been completed.
     */
    public void setProgressSilent(long progress) {
        synchronized (this) {
            this.progress = Math.min(progress, total);
        }
    }

    /**
     * Get the current progress.
     * @return The number of steps that have been completed.
     */
    public long getProgress() {
        return this.progress;
    }

    /**
     * Logs the finished progress bar and a newline to allow for further logging.
     */
    public void finish() {
        progress = total;
        logger.log();
        System.out.println();
    }
}
