package com.google.gwt.user.client.rpc;

@Deprecated
public class SerializationException extends Exception {
    public SerializationException() {
    }

    public SerializationException(String msg) {
        super(msg);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }
}
