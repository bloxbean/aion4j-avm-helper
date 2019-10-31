package org.aion4j.avm.helper;

import org.aion.types.AionAddress;
import org.aion.types.InternalTransaction;
import org.aion.types.Transaction;
import org.aion4j.avm.helper.crypto.AccountKey;
import org.aion4j.avm.helper.crypto.KeyHelper;
import org.aion4j.avm.helper.local.InvokableTxUtil;
import org.aion4j.avm.helper.signing.SignedInvokableTransactionBuilder;
import org.aion4j.avm.helper.util.HexUtil;
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

        SignedInvokableTransactionBuilder signedInvokableTransactionBuilder = new SignedInvokableTransactionBuilder();
        byte[] invokable = signedInvokableTransactionBuilder.privateKey(walletPrivateKey)
                .destination(ZERO_ADDRESS.toString())
                .executor(ZERO_ADDRESS.toString())
                .data(null)
                .senderNonce(BigInteger.ZERO)
                .value(BigInteger.ZERO)
                .buildSignedInvokableTransaction();

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

        SignedInvokableTransactionBuilder signedInvokableTransactionBuilder = new SignedInvokableTransactionBuilder();
        byte[] invokable = signedInvokableTransactionBuilder.privateKey(walletPrivateKey)
                .destination(null)
                .executor(executor.toString())
                .data(HexUtil.bytesToHexString(tx.copyOfTransactionData()))
                .senderNonce(tx.nonce)
                .value(tx.value)
                .buildSignedInvokableTransaction();

        System.out.println(HexUtil.bytesToHexString(invokable));

        InternalTransaction tx2 = InvokableTxUtil.decode(invokable, ZERO_ADDRESS, 1L, 1L);

        assertNotNull(tx2);
        assertEquals(tx.destinationAddress, tx2.destination);
        assertEquals(tx.nonce, tx2.senderNonce);
        assertEquals(tx.value, tx2.value);
        assertArrayEquals(tx.copyOfTransactionData(), tx2.copyOfData());
    }

    @Test
    public void encodeDecodeTestWithProperAddress() throws Exception {
        AccountKey accountKey = KeyHelper.generatePrivateKey();
        byte[] address = KeyHelper.deriveAddress(accountKey.getPrivateKeyBytes());
        String walletPrivateKey = KeyHelper.generateWalletPrivateKey(accountKey);

        Transaction tx = Transaction.contractCallTransaction(
                new AionAddress(address),
                new AionAddress(HexUtil.hexStringToBytes("0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca")),
                new byte[0],
                BigInteger.ZERO,
                BigInteger.ZERO,
                new byte[0],
                1L,
                1L);

        System.out.println("Address: " + HexUtil.bytesToHexString(address));
        SignedInvokableTransactionBuilder signedInvokableTransactionBuilder = new SignedInvokableTransactionBuilder();
        byte[] invokable = signedInvokableTransactionBuilder.privateKey(walletPrivateKey)
                .destination("0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca")
                .executor(HexUtil.bytesToHexString(address))
                .data(null)
                .senderNonce(BigInteger.ZERO)
                .value(BigInteger.ZERO)
                .buildSignedInvokableTransaction();

        System.out.println("Using sign: " + HexUtil.bytesToHexString(invokable));

//        invokable = InvokableTxUtil.encodeInvokableTransaction(walletPrivateKey, BigInteger.ZERO,
//                new AionAddress(HexUtil.hexStringToBytes("0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca")), BigInteger.ZERO,
//                new byte[0], new AionAddress(address));
//
//        System.out.println("Invoke util: " + HexUtil.bytesToHexString(invokable));

        InternalTransaction tx2 = InvokableTxUtil.decode(invokable,
                new AionAddress(address),
                21000, 1);

        assertNotNull(tx2);
        assertEquals(tx.destinationAddress, tx2.destination);
        assertEquals(tx.nonce, tx2.senderNonce);
        assertEquals(tx.value, tx2.value);

        assertArrayEquals(tx.copyOfTransactionData(), tx2.copyOfData());
    }

}
