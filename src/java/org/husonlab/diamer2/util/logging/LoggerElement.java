package org.husonlab.diamer2.util.logging;

/**
 * Abstract class for elements that can be added to a {@link Logger}.
 */
public abstract class LoggerElement {

    Logger logger;

    void setLogger(Logger logger) {
        this.logger = logger;
    };
    Logger getLogger() {
        return logger;
    }
    abstract String getMessage();
}
