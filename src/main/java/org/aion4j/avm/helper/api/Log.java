package org.aion4j.avm.helper.api;

public interface Log {

    public void info(String message);
    public void debug(String message);
    public void info(String msg, Throwable t);
    public void debug(String msg, Throwable t);
    public void error(String msg, Throwable t);

    public boolean isDebugEnabled();
}
