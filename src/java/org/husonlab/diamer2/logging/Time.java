package org.husonlab.diamer2.logging;

public class Time extends LoggerElement {

    @Override
    String getMessage() {
        return "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "]";
    }
}
