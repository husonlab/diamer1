package org.husonlab.diamer2.logging;

import java.util.LinkedList;

public class Logger {

    private final String prefix;
    private final boolean withTime;
    private final long intervalTime;
    private long lastLogTime;
    private LinkedList<LoggerElement> loggerElements;
    private LogType logType;

    public Logger(String prefix, long intervalTime, boolean withTime) {
        this.prefix = "[" + prefix + "]";
        this.intervalTime = intervalTime;
        this.withTime = withTime;
        loggerElements = new LinkedList<>();
        logType = LogType.OVERWRITE;
    }

    public Logger(String prefix, boolean withTime) {
        this.prefix = "[" + prefix + "]";
        this.intervalTime = 0;
        this.withTime = withTime;
        logType = LogType.OVERWRITE;
    }

    public void notifyProgress() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > intervalTime) {
            log();
        }
    }

    public Logger addElement(LoggerElement loggerElement) {
        if (loggerElement.getLogType() == LogType.NEWLINE) {
            logType = LogType.NEWLINE;
        }
        loggerElements.add(loggerElement);
        loggerElement.setLogger(this);
        return this;
    }

    public void log() {
        lastLogTime = System.currentTimeMillis();
        StringBuilder log = new StringBuilder().append(prefix);
        for (LoggerElement loggerElement : loggerElements) {
            log.append(loggerElement.getMessage()).append(" ");
        }
        if (logType == LogType.NEWLINE) {
            System.out.println(log);
        } else {
            System.out.print("\r" + log);
        }
    }

    public void logInfo(String message) {
        System.out.println("[INFO]" + prefix + getTimeString() + " " + message);
    }

    public void logWarning(String message) {
        System.err.println("[WARN]" + prefix + getTimeString() + " " + message);
    }

    public void logError(String message) {
        System.err.println("[ERROR]" + prefix + getTimeString() + " " + message);
    }

    private String getTimeString() {
        if (withTime) {
            return "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "]";
        }
        return "";
    }

}

