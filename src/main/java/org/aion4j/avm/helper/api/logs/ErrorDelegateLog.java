package org.aion4j.avm.helper.api.logs;

import org.aion4j.avm.helper.api.Log;

/**
 * This logger implementation only shows error
 */
public class ErrorDelegateLog implements Log {

    private Log delegate;

    public ErrorDelegateLog(Log log) {
        this.delegate = log;
    }

    @Override
    public void info(String message) {

    }

    @Override
    public void debug(String message) {

    }

    @Override
    public void error(String message) {
        delegate.error(message);
    }

    @Override
    public void warn(String message) {
        delegate.warn(message);
    }

    @Override
    public void info(String msg, Throwable t) {

    }

    @Override
    public void debug(String msg, Throwable t) {

    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(msg, t);
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }
}
