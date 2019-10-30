package org.aion4j.avm.helper;

import org.aion.types.AionAddress;
import org.aion.types.InternalTransaction;
import org.aion.types.Transaction;
import org.aion4j.avm.helper.crypto.AccountKey;
import org.aion4j.avm.helper.crypto.KeyHelper;
import org.aion4j.avm.helper.metatxn.InvokableTxUtil;
import org.junit.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.*;

public class InvokableTxUtilTest {
    private static AionAddress ZERO_ADDRESS = new AionAddress(new byte[32]);

    @Test
    public void encodeDecodeTest() throws Exception {
        AccountKey accountKey = KeyHelper.generatePrivateKey();
        byte[] address = KeyHelper.deriveAddress(accountKey.getPrivateKeyBytes());
        String walletPrivateKey = KeyHelper.generateWalletPrivateKey(accountKey);

        Transaction tx = Transaction.contractCallTransaction(
                new AionAddress(address),
                ZERO_ADDRESS,
                new byte[0],
                BigInteger.ZERO,
                BigInteger.ZERO,
                new byte[0],
                1L,
                1L);

        AionAddress executor = ZERO_ADDRESS;

        byte[] invokable =
                InvokableTxUtil.encodeInvokableTransaction(
                        walletPrivateKey,
                        tx.nonce,
                        tx.destinationAddress,
                        tx.value,
                        tx.copyOfTransactionData(),
                        executor);

        InternalTransaction tx2 = InvokableTxUtil.decode(invokable, ZERO_ADDRESS, 21000, 1);

        assertNotNull(tx2);
        assertEquals(tx.destinationAddress, tx2.destination);
        assertEquals(tx.nonce, tx2.senderNonce);
        assertEquals(tx.value, tx2.value);
        assertArrayEquals(tx.copyOfTransactionData(), tx2.copyOfData());
    }

    @Test
    public void encodeDecodeContractCreateTest() throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        AccountKey accountKey = KeyHelper.generatePrivateKey();
        byte[] address = KeyHelper.deriveAddress(accountKey.getPrivateKeyBytes());
        String walletPrivateKey = KeyHelper.generateWalletPrivateKey(accountKey);

        Transaction tx = Transaction.contractCreateTransaction(
                new AionAddress(address),
                new byte[0],
                BigInteger.ZERO,
                BigInteger.ZERO,
                new byte[0],
                1L,
                1L);

        AionAddress executor = ZERO_ADDRESS;

        byte[] invokable =
                InvokableTxUtil.encodeInvokableTransaction(
                        walletPrivateKey,
                        tx.nonce,
                        tx.destinationAddress,
                        tx.value,
                        tx.copyOfTransactionData(),
                        executor);

        InternalTransaction tx2 = InvokableTxUtil.decode(invokable, ZERO_ADDRESS, 1L, 1L);

        assertNotNull(tx2);
        assertEquals(tx.destinationAddress, tx2.destination);
        assertEquals(tx.nonce, tx2.senderNonce);
        assertEquals(tx.value, tx2.value);
        assertArrayEquals(tx.copyOfTransactionData(), tx2.copyOfData());
    }

}
