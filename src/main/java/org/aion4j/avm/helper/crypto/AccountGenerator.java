package org.aion4j.avm.helper.crypto;

import org.aion4j.avm.helper.util.HexUtil;

import java.security.InvalidAlgorithmParameterException;
import java.security.spec.InvalidKeySpecException;

/**
 *
 * @author Satya
 */
public class AccountGenerator {

    public static Account newAddress() {
        AccountKey accountKey = KeyHelper.generatePrivateKey();
        try {
            byte[] addressByte = KeyHelper.deriveAddress(accountKey.getPrivateKeyBytes());

            String privateKey = KeyHelper.generateWalletPrivateKey(accountKey);

            Account account = new Account();
            account.setPrivateKey(privateKey);
            account.setAddress(HexUtil.bytesToHexString(addressByte));

            return account;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }

    }


    public static  void main(String[] args) throws InvalidAlgorithmParameterException, InvalidKeySpecException {
        Account account = new AccountGenerator().newAddress();

        System.out.println(account.getPrivateKey());
        System.out.printf(account.getAddress());

    }
}