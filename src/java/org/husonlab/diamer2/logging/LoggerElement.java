package org.husonlab.diamer2.logging;

public interface LoggerElement {
    Logger getLogger();
    String getMessage();
    LogType getLogType();
    void setLogger(Logger logger);
}
