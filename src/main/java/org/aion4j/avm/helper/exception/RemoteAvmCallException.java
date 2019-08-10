package org.aion4j.avm.helper.exception;

public class RemoteAvmCallException extends Exception {

    public RemoteAvmCallException(String message) {
        super(message);
    }

    public RemoteAvmCallException(String message, Exception e) {
        super(message, e);
    }
}
