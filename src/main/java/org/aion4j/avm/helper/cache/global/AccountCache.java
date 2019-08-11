package org.aion4j.avm.helper.cache.global;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.aion4j.avm.helper.crypto.Account;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties
public class AccountCache {

    private List<Account> accounts;

    public AccountCache() {
        accounts = new ArrayList<>();
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public void addAccount(Account account) {
        if(accounts == null)
            accounts = new ArrayList<>();

        if(account == null)
            return;

        accounts.add(account);
    }
}
