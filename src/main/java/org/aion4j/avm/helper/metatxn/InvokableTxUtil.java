package org.aion4j.avm.helper.metatxn;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import org.aion.avm.embed.crypto.CryptoUtil;
import org.aion.avm.embed.crypto.Ed25519Signature;
import org.aion.avm.embed.crypto.ISignature;
import org.aion.rlp.RLP;
import org.aion.rlp.RLPList;
import org.aion.types.AionAddress;
import org.aion.types.InternalTransaction;
import org.aion.types.InternalTransaction.RejectedStatus;
import org.aion4j.avm.helper.crypto.KeyHelper;
import org.aion4j.avm.helper.signing.Blake2b;
import org.aion4j.avm.helper.signing.SignedTransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * This class is originally taken from Aion code base https://github.com/aionnetwork/aion/blob/master/modBase/src/org/aion/base/InvokableTxUtil.java
 * Modified as required by aion4j
 */
public class InvokableTxUtil {

    private static final byte VERSION = 0;
    private static final Logger LOG = LoggerFactory.getLogger(InvokableTxUtil.class);
    private static AionAddress ZERO_ADDRESS = new AionAddress(new byte[32]);

    private static final int
            RLP_META_TX_NONCE = 0,
            RLP_META_TX_TO = 1,
            RLP_META_TX_VALUE = 2,
            RLP_META_TX_DATA = 3,
            RLP_META_TX_EXECUTOR = 4,
            RLP_META_TX_SIG = 5;

    /**
     * Encodes an invokable transaction. The first byte will be the version code.
     * Currently there is only version 0.
     *
     */
    public static byte[] encodeInvokableTransaction(
            String privateKey,
            BigInteger nonce,
            AionAddress destination,
            BigInteger value,
            byte[] data,
            AionAddress executor) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        if (privateKey == null) {
            throw new NullPointerException("Key cannot be null");
        }
        if (nonce == null) {
            throw new NullPointerException("Nonce cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("Value cannot be null");
        }
        if (data == null) {
            throw new NullPointerException("Data cannot be null");
        }

        EdDSAPrivateKey key = SignedTransactionBuilder.getEDSAPrivateKey(privateKey);

        byte[] rlpEncodingWithoutSignature =
                rlpEncodeWithoutSignature(
                        nonce,
                        destination,
                        value,
                        data,
                        executor);

        byte[] signBytes = SignedTransactionBuilder.sign(key, blake2b(prependVersion(rlpEncodingWithoutSignature)));
        ISignature signature = Ed25519Signature.fromPublicKeyAndSignature(key.getAbyte(), signBytes);

        byte[] encoding =
                rlpEncode(
                        nonce,
                        destination,
                        value,
                        data,
                        executor,
                        signature);

        // prepend version code
        return prependVersion(encoding);
    }

    public static InternalTransaction decode(byte[] encodingWithVersion, AionAddress callingAddress, long energyPrice, long energyLimit) {
        // Right now we only have version 0, so if it is not 0, return null
        if (encodingWithVersion[0] != 0) { return null; }

        byte[] rlpEncoding = Arrays.copyOfRange(encodingWithVersion, 1, encodingWithVersion.length);

        RLPList decodedTxList;
        try {
            decodedTxList = RLP.decode2(rlpEncoding);
        } catch (Exception e) {
            return null;
        }
        RLPList tx = (RLPList) decodedTxList.get(0);

        BigInteger nonce = new BigInteger(1, tx.get(RLP_META_TX_NONCE).getRLPData());
        BigInteger value = new BigInteger(1, tx.get(RLP_META_TX_VALUE).getRLPData());
        byte[] data = tx.get(RLP_META_TX_DATA).getRLPData();

        AionAddress destination;
        try {
            destination = new AionAddress(tx.get(RLP_META_TX_TO).getRLPData());
        } catch(Exception e) {
            destination = null;
        }

        AionAddress executor;
        try {
            executor = new AionAddress(tx.get(RLP_META_TX_EXECUTOR).getRLPData());
        } catch(Exception e) {
            executor = null;
        }

        // Verify the executor

        if (executor != null && !executor.equals(ZERO_ADDRESS) && !executor.equals(callingAddress)) {
            return null;
        }

        byte[] sigs = tx.get(RLP_META_TX_SIG).getRLPData();
        Ed25519Signature signature;
        AionAddress sender;
        if (sigs != null) {
            // Singature Factory will decode the signature based on the algo
            // presetted in main() entry.
            Ed25519Signature is = Ed25519Signature.fromCombinedPublicKeyAndSignature(sigs);
            if (is != null) {
                signature = is;
                byte[] address = KeyHelper.computeA0Address(is.getPublicKey(new byte[0]));
                sender = new AionAddress(address);
            } else {
                return null;
            }
        } else {
            return null;
        }

        try {
            return createFromRlp(
                    nonce,
                    sender,
                    destination,
                    value,
                    data,
                    executor,
                    energyLimit,
                    energyPrice,
                    signature,
                    encodingWithVersion);
        }
        catch (Exception e) {
            LOG.error("Invokable tx -> unable to decode rlpEncoding. " + e);
            return null;
        }
    }

    private static byte[] rlpEncodeWithoutSignature(
            BigInteger nonce,
            AionAddress destination,
            BigInteger value,
            byte[] data,
            AionAddress executor) {

        byte[] nonceEncoded = RLP.encodeBigInteger(nonce);
        byte[] destinationEncoded = RLP.encodeElement(destination == null ? null : destination.toByteArray());
        byte[] valueEncoded = RLP.encodeBigInteger(value);
        byte[] dataEncoded = RLP.encodeElement(data);
        byte[] executorEncoded = RLP.encodeElement(executor == null ? null : executor.toByteArray());

        return RLP.encodeList(
                nonceEncoded,
                destinationEncoded,
                valueEncoded,
                dataEncoded,
                executorEncoded);
    }

    private static byte[] rlpEncode(
            BigInteger nonce,
            AionAddress destination,
            BigInteger value,
            byte[] data,
            AionAddress executor,
            ISignature signature) {

        byte[] nonceEncoded = RLP.encodeBigInteger(nonce);
        byte[] destinationEncoded = RLP.encodeElement(destination == null ? null : destination.toByteArray());
        byte[] valueEncoded = RLP.encodeBigInteger(value);
        byte[] dataEncoded = RLP.encodeElement(data);
        byte[] executorEncoded = RLP.encodeElement(executor == null ? null : executor.toByteArray());
        byte[] signatureEncoded = RLP.encodeElement(combinePublicKeyAndSignature(signature));

        return RLP.encodeList(
                nonceEncoded,
                destinationEncoded,
                valueEncoded,
                dataEncoded,
                executorEncoded,
                signatureEncoded);
    }

    private static byte[] combinePublicKeyAndSignature(ISignature signature) {
        int publicKeyLength = signature.getPublicKey(null).length;
        int sigLength = signature.getSignature().length;

        byte[] buf = new byte[publicKeyLength + sigLength];
        System.arraycopy(signature.getPublicKey(null), 0, buf, 0, publicKeyLength);
        System.arraycopy(signature.getSignature(), 0, buf, publicKeyLength, sigLength);
        return buf;
    }

    private static InternalTransaction createFromRlp(
            BigInteger nonce,
            AionAddress sender,
            AionAddress destination,
            BigInteger value,
            byte[] data,
            AionAddress executor,
            long energyLimit,
            long energyPrice,
            ISignature signature,
            byte[] rlpEncodingWithVersion) {

        byte[] transactionHashWithoutSignature =
                blake2b(
                        prependVersion(
                                InvokableTxUtil.rlpEncodeWithoutSignature(
                                        nonce,
                                        destination,
                                        value,
                                        data,
                                        executor)));

        if (!CryptoUtil.verifyEdDSA(transactionHashWithoutSignature, signature.getSignature(), signature.getPublicKey(null))) {
            throw new IllegalStateException("Signature does not match Transaction Content");
        }

        byte[] transactionHash = blake2b(rlpEncodingWithVersion);

        if (destination == null) {
            return
                    InternalTransaction.contractCreateInvokableTransaction(
                            RejectedStatus.NOT_REJECTED,
                            sender,
                            nonce,
                            value,
                            data,
                            energyLimit,
                            energyPrice,
                            transactionHash);
        } else {
            return
                    InternalTransaction.contractCallInvokableTransaction(
                            RejectedStatus.NOT_REJECTED,
                            sender,
                            destination,
                            nonce,
                            value,
                            data,
                            energyLimit,
                            energyPrice,
                            transactionHash);
        }
    }

    private static byte[] prependVersion(byte[] encoding) {
        byte[] ret = new byte[encoding.length + 1];
        ret[0] = VERSION;
        System.arraycopy(encoding, 0, ret, 1, encoding.length);
        return ret;
    }

    private static byte[] blake2b(byte[] msg) {
        Blake2b.Digest digest = Blake2b.Digest.newInstance(32);
        digest.update(msg);
        return digest.digest();
    }
}
