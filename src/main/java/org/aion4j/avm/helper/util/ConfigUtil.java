package org.aion4j.avm.helper.util;

public class ConfigUtil {
    public static final String ENABLE_VERBOSE_CONCURRENT_EXECUTOR = "enableVerboseConcurrentExecutor";
    public static final String ENABLE_VERBOSE_CONTRACT_ERRORS = "enableVerboseContractErrors";
    public static final String PRESERVE_DEBUGGABILITY = "preserveDebuggability";

    public static String getProperty(String name) {

        String value = System.getProperty(name);

        if(value != null && !value.isEmpty())
            return value;
        else {
            name = name.replace(".", "_");
            return System.getenv(name);
        }
    }

    public static boolean getAvmConfigurationBooleanProps(String name, boolean defaultValue) {

        String value = System.getProperty(name);

        if(value != null && !value.isEmpty())
            return Boolean.parseBoolean(value);
        else {
            name = name.replace(".", "_");
            String envValue = System.getenv(name);

            if(envValue == null)
                return defaultValue;
            else
                return Boolean.parseBoolean(envValue);
        }
    }
}
