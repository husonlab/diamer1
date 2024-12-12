package org.husonlab.diamer2.logging;

public class OneLineLogger extends Logger {

    private final long intervalTime;
    private long lastLogTime;

    public OneLineLogger(String prefix, long intervalTime) {
        super(prefix);
        this.intervalTime = intervalTime;
    }

    public void notifyProgress() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > intervalTime) {
            log();
        }
    }

    protected String getMessage() {
        return "\n" + super.getMessage() + "\n";
    }

    public void log() {
        System.out.print("\r" + super.getMessage());
    }
}
