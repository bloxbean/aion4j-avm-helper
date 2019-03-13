package org.aion4j.avm.helper.exception;

public class LocalAVMException extends RuntimeException {

    public LocalAVMException(String msg) {
        super(msg);
    }

    public LocalAVMException(Exception e) {
        super(e);
    }
}
