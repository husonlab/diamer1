package org.husonlab.diamer2.util.logging;

public abstract class LoggerElement {

    Logger logger;

    Logger getLogger() {
        return logger;
    }
    abstract String getMessage();
    void setLogger(Logger logger) {
        this.logger = logger;
    };
}
