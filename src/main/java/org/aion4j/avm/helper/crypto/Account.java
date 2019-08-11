package org.aion4j.avm.helper.crypto;

import java.util.Objects;

/**
 *
 * @author Satya
 */
public class Account {

    private String privateKey;
    private String address;

    public Account() {

    }

    public Account(String address, String privateKey) {
        this.address = address;
        this.privateKey = privateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return privateKey.equals(account.privateKey) &&
                address.equals(account.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKey, address);
    }

    @Override
    public String toString() {
        return "Account{" +
                "privateKey=xxxxxxxxxxxxx" +
                 ", address='" + address + '\'' +
                '}';
    }
}
