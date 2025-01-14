package org.husonlab.diamer2.util.logging;

/**
 * {@link Logger} that updates its message on the same line after a certain time interval.
 * <p>
 * Important: The class only checks the time interval when {@link #notifyUpdate()}is called (e.g. by an associated
 * {@link LoggerElement}).
 */
public class OneLineLogger extends Logger {

    private final long intervalTime;
    private long lastLogTime;

    /**
     * Constructs a new {@link OneLineLogger} with the given prefix and interval time.
     * @param prefix The prefix of the log message (e.g. the name of the Class that is using it).
     * @param intervalTime The time interval in milliseconds after which the message is updated.
     */
    public OneLineLogger(String prefix, long intervalTime) {
        super(prefix);
        this.intervalTime = intervalTime;
    }

    /**
     * Notifies the {@link OneLineLogger} that the message of one of its associated {@link LoggerElement}s has changed.
     * <p>
     * The time interval is only checked when this method is called. A call to this method can also be used to force an
     * update of the message.
     */
    public void notifyUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > intervalTime) {
            log();
            lastLogTime = currentTime;
        }
    }

    /**
     * Logs the current message to the console.
     */
    public void log() {
        System.out.print("\r[INFO]" + super.getMessage());
    }
}
