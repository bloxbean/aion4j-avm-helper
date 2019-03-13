package org.aion4j.avm.helper.api;

public interface Log {

    public void info(String message);
    public void debug(String message);

    public boolean isDebugEnabled();
}
