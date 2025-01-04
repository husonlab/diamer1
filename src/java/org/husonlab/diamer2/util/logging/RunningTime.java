package org.husonlab.diamer2.util.logging;

public class RunningTime extends LoggerElement {

    private final long startTime;

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
