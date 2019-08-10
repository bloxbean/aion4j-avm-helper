package org.aion4j.avm.helper.api.logs;

import org.aion4j.avm.helper.api.Log;

/**
 * This Log implementation ignores all logs
 */
public class DummyLog implements Log {

    @Override
    public void info(String s) {

    }

    @Override
    public void debug(String s) {

    }

    @Override
    public void error(String message) {

    }

    @Override
    public void warn(String message) {

    }

    @Override
    public void info(String s, Throwable throwable) {

    }

    @Override
    public void debug(String s, Throwable throwable) {

    }

    @Override
    public void error(String s, Throwable throwable) {

    }

    @Override
    public void warn(String msg, Throwable t) {

    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }
}
