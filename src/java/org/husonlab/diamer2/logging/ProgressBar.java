package org.husonlab.diamer2.logging;

public class ProgressBar implements LoggerElement {

    private Logger logger;
    private final long total;
    private long current;
    private final int length;

    public ProgressBar(long total, int length) {
        this.total = total;
        this.length = length;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setProgress(long current) {
        this.current = current;
        this.logger.notifyProgress();
    }

    @Override
    public Logger getLogger() {
        return null;
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
        this.logger.log();
        System.out.println();
    }

    @Override
    public LogType getLogType() {
        return LogType.OVERWRITE;
    }
}
