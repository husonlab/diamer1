package org.husonlab.diamer.util.logging;

import java.util.LinkedList;

/**
 * Class that logs the messages of the associated {@link LoggerElement}s.
 * <p>
 * A log can be triggered either by an update of a {@link LoggerElement} or by calling on of the {@link Logger#log()},
 * {@link Logger#logInfo(String)}, {@link Logger#logWarning(String)} or {@link Logger#logError(String)} methods.
 */
public class Logger {

    protected final String prefix;
    protected LinkedList<LoggerElement> loggerElements;

    /**
     * Constructs a new {@link Logger} with the given prefix.
     * @param prefix The prefix of the {@link Logger} (e.g. the name of the Class that is using the logger).
     */
    public Logger(String prefix) {
        loggerElements = new LinkedList<>();
        this.prefix = "[" + prefix + "]";
    }

    /**
     * Adds a new {@link LoggerElement} to the {@link Logger}.
     * @param loggerElement The {@link LoggerElement} to add.
     * @return The {@link Logger} itself.
     */
    public Logger addElement(LoggerElement loggerElement) {
        loggerElements.add(loggerElement);
        loggerElement.setLogger(this);
        return this;
    }

    /**
     * Method that is called by a {@link LoggerElement} to notify the {@link Logger} that it has been updated.
     * <p>
     * Can also be called manually to trigger a log.
     */
    public void notifyUpdate() {
        log();
    }

    /**
     * @return The message of the {@link Logger} containing the messages of all associated {@link LoggerElement}s.
     */
    protected String getMessage() {
        StringBuilder log = new StringBuilder().append(prefix);
        for (LoggerElement loggerElement : loggerElements) {
            log.append(loggerElement.getMessage());
        }
        return log.toString();
    }

    public String toString() {
        return getMessage();
    }

    /**
     * Logs the messages of all associated {@link LoggerElement}s.
     */
    public void log() {
        System.out.println(getMessage());
    }

    /**
     * Logs the given message with the tag [INFO].
     * @param message The message to log.
     */
    public void logInfo(String message) {
        System.out.println("[INFO]" + getMessage() + " " + message);
    }

    /**
     * Logs the given message with the tag [WARNING].
     * @param message The message to log.
     */
    public void logWarning(String message) {
        System.out.println("[WARNING]" + getMessage() + " " + message);
    }

    /**
     * Logs the given message with the tag [ERROR].
     * @param message The message to log.
     */
    public void logError(String message) {
        System.out.println("[ERROR]" + getMessage() + " " + message);
    }
}

