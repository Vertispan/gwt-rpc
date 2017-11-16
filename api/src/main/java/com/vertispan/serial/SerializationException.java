package com.vertispan.serial;

/**
 * Base exception type for errors relating to the serialization stream.
 */
public class SerializationException extends com.google.gwt.user.client.rpc.SerializationException {

    public SerializationException() {
    }

    public SerializationException(String msg) {
        super(msg);
    }

    public SerializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }
}
