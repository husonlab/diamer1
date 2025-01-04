package org.husonlab.diamer2.util.logging;

public class Message extends LoggerElement {
    private String message;

    public Message(String message) {
        this.message = message;
    }

    @Override
    String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
