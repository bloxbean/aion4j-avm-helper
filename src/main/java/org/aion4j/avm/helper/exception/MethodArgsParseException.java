package org.aion4j.avm.helper.exception;

public class MethodArgsParseException extends Exception {

    public MethodArgsParseException(String msg) {
        super(msg);
    }

    public MethodArgsParseException(String msg, Exception ex) {
        super(msg, ex);
    }
}
