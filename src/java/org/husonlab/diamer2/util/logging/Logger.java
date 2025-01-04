package org.husonlab.diamer2.util.logging;

import java.util.LinkedList;

public class Logger {

    protected final String prefix;
    protected LinkedList<LoggerElement> loggerElements;

    public Logger(String prefix) {
        loggerElements = new LinkedList<>();
        this.prefix = "[" + prefix + "]";
    }

    public Logger addElement(LoggerElement loggerElement) {
        loggerElements.add(loggerElement);
        loggerElement.setLogger(this);
        return this;
    }

    public void notifyProgress() {
        log();
    }

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

    public void log() {
        System.out.println(getMessage());
    }

    public void logInfo(String message) {
        System.out.println("[INFO]" + getMessage() + " " + message);
    }

    public void logWarning(String message) {
        System.out.println("[WARNING]" + getMessage() + " " + message);
    }

    public void logError(String message) {
        System.out.println("[ERROR]" + getMessage() + " " + message);
    }

}

