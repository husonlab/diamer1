package org.husonlab.diamer2.util.logging;

public class ProgressBar extends LoggerElement {

    private long total;
    private long current;
    private final int length;

    public ProgressBar(long total, int length) {
        this.total = total;
        this.length = length;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setProgress(long current) {
        this.current = Math.min(current, total);
        this.logger.notifyProgress();
    }

    @Override
    public String getMessage() {
        if (total > 0) {
            int percent = (int) (100.0 * current / total);
            int bars = percent * length / 100;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append("=".repeat(Math.max(0, bars)));
            sb.append(" ".repeat(Math.max(0, length - bars)));
            sb.append("] ");
            sb.append(percent);
            sb.append("%");
            return sb.toString();
        }
        return "";
    }

    public void finish() {
        this.current = this.total;
        logger.log();
        System.out.println();
    }

    public long getProgress() {
        return this.current;
    }

    public void incrementProgress() {
        this.current++;
        this.logger.notifyProgress();
    }

    public void incrementProgress(int amount) {
        this.current += amount;
        this.logger.notifyProgress();
    }
}
