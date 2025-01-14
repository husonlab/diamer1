package org.husonlab.diamer2.util.logging;

/**
 * {@link LoggerElement} to log the current time as [HH:mm].
 */
public class Time extends LoggerElement {

    @Override
    String getMessage() {
        return "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "]";
    }
}
