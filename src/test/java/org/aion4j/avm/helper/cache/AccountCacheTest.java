package org.aion4j.avm.helper.cache;

import org.aion4j.avm.helper.api.logs.Slf4jLog;
import org.aion4j.avm.helper.cache.global.AccountCache;
import org.aion4j.avm.helper.cache.global.GlobalCache;
import org.aion4j.avm.helper.crypto.Account;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AccountCacheTest {
    private final static Logger logger = LoggerFactory.getLogger(AccountCacheTest.class);

    String targetFolder = null;
    String accountCacheFile = ".aion4j.account.conf";

    @Before
    public void setup() throws IOException {
        targetFolder = Files.createTempDirectory("aion4j-test").toFile().getAbsolutePath();
        System.out.println(targetFolder);
    }

    @After
    public void tearDown() {
        new File(targetFolder, accountCacheFile).delete();
        new File(targetFolder).delete();
    }

    @Test
    public void testAddAccount() {
        Account account1 = new Account("a-address", "a-privatekey");
        Account account2 = new Account("b-address", "b-privatekey");
        Account account3 = new Account("c-address", "c-privatekey");

        GlobalCache globalCache = new GlobalCache(targetFolder, new Slf4jLog(AccountCacheTest.class));
        AccountCache accountCache = globalCache.getAccountCache();

        accountCache.addAccount(account1);
        accountCache.addAccount(account2);
        globalCache.setAccountCache(accountCache);

        //Read now
        accountCache = globalCache.getAccountCache();

        Assert.assertEquals(2, accountCache.getAccounts().size());
        Assert.assertEquals(account1, accountCache.getAccounts().get(0));
        Assert.assertEquals(account2, accountCache.getAccounts().get(1));

        //write a new one
        accountCache.getAccounts().add(account3);
        globalCache.setAccountCache(accountCache);

        //Read now
        accountCache = globalCache.getAccountCache();

        Assert.assertEquals(3, accountCache.getAccounts().size());
        Assert.assertEquals(account1, accountCache.getAccounts().get(0));
        Assert.assertEquals(account2, accountCache.getAccounts().get(1));
        Assert.assertEquals(account3, accountCache.getAccounts().get(2));

    }

    @Test
    public void testAccountCacheClear() {
        Account account1 = new Account("a-address", "a-privatekey");
        Account account2 = new Account("b-address", "b-privatekey");
        Account account3 = new Account("c-address", "c-privatekey");

        GlobalCache globalCache = new GlobalCache(targetFolder, new Slf4jLog(AccountCacheTest.class));
        AccountCache accountCache = globalCache.getAccountCache();

        accountCache.addAccount(account1);
        accountCache.addAccount(account2);
        globalCache.setAccountCache(accountCache);

        //Read now
        accountCache = globalCache.getAccountCache();

        Assert.assertEquals(2, accountCache.getAccounts().size());

        //Now clear
        globalCache.clearAccountCache();

        //Read now
        accountCache = globalCache.getAccountCache();
        Assert.assertEquals(0, accountCache.getAccounts().size());
    }
}
