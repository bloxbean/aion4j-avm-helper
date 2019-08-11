package org.aion4j.avm.helper.cache.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aion4j.avm.helper.api.Log;

import java.io.File;
import java.io.IOException;

public class GlobalCache {
    public static String ACCOUNT_CACHE = ".aion4j.account.conf";
    private final String targetFolder;
    private Log log;
    private ObjectMapper objectMapper;

    public GlobalCache(String targetFolder, Log log) {
        this.targetFolder = targetFolder;
        this.log = log;

        this.objectMapper = new ObjectMapper();
    }

    public void setAccountCache(AccountCache accountCache) {
        File file = getAccountCacheFile();
        try {
            objectMapper.writeValue(file, accountCache);
        } catch (Exception e) {
            log.warn("Could not write to account cache", e);
            if(log.isDebugEnabled()) {
                log.debug("Could not write to account cache", e);
            }
        }
    }

    public AccountCache getAccountCache() {
        File file = getAccountCacheFile();

        if(!file.exists())
            return new AccountCache();

        AccountCache accountCache = null;

        try {
            accountCache = objectMapper.readValue(file, AccountCache.class);
        } catch (Exception e) {
            accountCache = new AccountCache();
            //e.printStackTrace();
            log.warn("Could not read from account cache: " + e.getMessage());
            if(log.isDebugEnabled()) {
                log.error("Could not read from account cache", e);
            }
        }

        return accountCache;
    }

    public void clearAccountCache() {
        File file = getAccountCacheFile();
        file.delete();
    }

    private File getAccountCacheFile() {
        return new File(targetFolder, ACCOUNT_CACHE);
    }
}
