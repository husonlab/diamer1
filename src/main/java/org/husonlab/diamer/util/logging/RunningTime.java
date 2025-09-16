package org.husonlab.diamer.util.logging;

/**
 * {@link LoggerElement} that logs the running time of the program.
 * The time is calculated from the creation of the object.
 * The time is formatted as [HH:MM:SS].
 */
public class RunningTime extends LoggerElement {

    private final long startTime;

    /**
     * Constructs a new {@link RunningTime} object.
     */
    public RunningTime() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public String getMessage() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("[%02d:%02d:%02d]", hours, minutes % 60, seconds % 60);
    }
}
