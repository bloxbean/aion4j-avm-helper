package org.aion4j.avm.helper;

import org.aion4j.avm.helper.crypto.Account;
import org.aion4j.avm.helper.crypto.AccountGenerator;
import org.aion4j.avm.helper.crypto.KeyHelper;
import org.aion4j.avm.helper.util.HexUtil;
import org.junit.Test;

import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 *
 * @author Satya
 */
public class AccountGeneratorTest {

    @Test
    public void generateNewAddress() {
        Account account = AccountGenerator.newAddress();

        String privateKey = account.getPrivateKey();

        assertTrue(HexUtil.hexStringToBytes(privateKey).length == 64);
        assertTrue(account.getAddress().startsWith("a0"));
    }

    @Test
    public void generateNewAddressAndVerify() throws InvalidKeySpecException {
        String privateKey = "69d15044a4905760a47cb3eb17823f00121ae9e57cf5ca17c4c2d7533ba2080388e6b898489a8b48b84ef5b627669c5be8e2ce83f2baedc46332fd0708589d9d";
        String expectedAddress = "a018016170f3bede040c4bf8eb7619e632372d94cf7a94e5a3a0a499094ff9de";

        byte[] pvtKeyBytes = HexUtil.hexStringToBytes(privateKey);
        pvtKeyBytes = Arrays.copyOfRange(pvtKeyBytes, 0, 32);

        byte[] addressBytes = KeyHelper.deriveAddress(pvtKeyBytes);
        String address = HexUtil.bytesToHexString(addressBytes);

        assertTrue(expectedAddress.equals(address));
    }
}
