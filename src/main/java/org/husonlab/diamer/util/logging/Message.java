package org.husonlab.diamer.util.logging;

/**
 * Simple {@link LoggerElement} that only contains a message.
 */
public class Message extends LoggerElement {
    private String message;

    /**
     * Constructs a new {@link LoggerElement} with the given message.
     * @param message The message of the {@link Message}.
     */
    public Message(String message) {
        this.message = message;
    }

    @Override
    String getMessage() {
        return message;
    }

    /**
     * Updates the message.
     * @param message The new message.
     */
    public void setMessage(String message) {
        this.message = message;
        logger.notifyUpdate();
    }
}
