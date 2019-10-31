package org.aion4j.avm.helper.signing;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import org.aion.rlp.RLP;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import static net.i2p.crypto.eddsa.Utils.hexToBytes;

/**
 * A convenience class for building a transaction that can be run by a third party who will pay the
 * energy costs. This transaction is signed locally (offline) using a private key.
 *
 * In general, if a specific method is invoked multiple times before building the transaction, then
 * the last invocation takes precedence.
 *
 * The builder can be used to construct additional transactions after each build, and the previous
 * build settings will apply.
 *
 * The builder provides a {@code reset} method that will clear the build back to its initial state.
 *
 * The sender of the transaction will be the Aion account that corresponds to the provided private
 * key.
 */

public class SignedInvokableTransactionBuilder {
    private static final byte VERSION = (byte) 0;
    private static final byte AION_ADDRESS_PREFIX = (byte) 0xa0;

    // Required fields.
    private String privateKey = null;
    private BigInteger nonce = null;

    // Fields we provide default values for.
    private BigInteger value = null;
    private String destination = null;
    private String data = null;
    private String executor = null;

    /**
     * The private key used to sign the transaction with.
     *
     * <b>This field must be set.</b>
     *
     * @param privateKey The private key.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder privateKey(String privateKey) {
        if(privateKey != null && privateKey.startsWith("0x"))
            this.privateKey = privateKey.substring(2);
        else
            this.privateKey = privateKey;
        return this;
    }

    /**
     * The destination address of the transaction.
     *
     * <b>This field must be set.</b>
     *
     * @param destination The destination.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder destination(String destination) {
        if(destination != null && destination.startsWith("0x"))
            this.destination = destination.substring(2);
        else
            this.destination = destination;
        return this;
    }

    /**
     * The amount of value to transfer from the sender to the destination.
     *
     * @param value The amount of value to transfer.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder value(BigInteger value) {
        this.value = value;
        return this;
    }

    /**
     * The nonce of the sender.
     *
     * <b>This field must be set.</b>
     *
     * @param nonce The sender nonce.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder senderNonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }

    /**
     * The transaction data.
     *
     * @param data The data.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder data(String data) {
        if(data != null && data.startsWith("0x"))
            this.data = data.substring(2);
        else
            this.data = data;
        return this;
    }

    /**
     * The address of the contract that is allowed to invoke this transaction.
     * If this is null or the 0 address, it can be invoked by any contract
     *
     * @param executor The address of the executor.
     * @return this builder.
     */
    public SignedInvokableTransactionBuilder executor(String executor) {
        if(executor != null && executor.startsWith("0x"))
            this.executor = executor.substring(2);
        else
            this.executor = executor;
        return this;
    }

    /**
     * Constructs a transaction whose fields correspond to the fields as they have been set by the
     * provided builder methods, and signs this transaction with the provided private key.
     *
     * The following fields must be set prior to calling this method:
     *   - private key
     *   - nonce
     *
     * The following fields, if not set, will have the following default values:
     *   - value: {@link BigInteger#ZERO}
     *   - destination: empty array
     *   - executor: empty array
     *   - data: empty array
     *
     * @return the bytes of the signed transaction.
     */
    public byte[] buildSignedInvokableTransaction() throws InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        if (this.privateKey == null) {
            throw new IllegalStateException("No private key specified.");
        }
        if (this.nonce == null) {
            throw new IllegalStateException("No nonce specified.");
        }

        EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(addSkPrefix(this.privateKey)));

        byte[] publicKey = privateKey.getAbyte();
        byte[] addrBytes = blake2b(publicKey);
        addrBytes[0] = AION_ADDRESS_PREFIX;

        byte[] _to = (this.destination == null) ? new byte[0] : hexToBytes(this.destination);
        byte[] _executor = (this.executor == null) ? new byte[0] : hexToBytes(this.executor);

        byte[] encodedNonce = RLP.encodeBigInteger(this.nonce);
        byte[] encodedTo = RLP.encodeElement(_to);
        byte[] encodedValue = RLP.encodeBigInteger((this.value == null) ? BigInteger.ZERO : this.value);
        byte[] encodedData = RLP.encodeElement((this.data == null) ? new byte[0] : hexToBytes(this.data));
        byte[] encodedExecutor = RLP.encodeElement(_executor);

        byte[] fullEncoding = RLP.encodeList(encodedNonce, encodedTo, encodedValue, encodedData, encodedExecutor);

        byte[] rawHash = blake2b(prependVersion(fullEncoding));
        byte[] signatureOnly = sign(privateKey, rawHash);
        byte[] preEncodeSignature = new byte[publicKey.length + signatureOnly.length];
        System.arraycopy(publicKey, 0, preEncodeSignature, 0, publicKey.length);
        System.arraycopy(signatureOnly, 0, preEncodeSignature, publicKey.length, signatureOnly.length);
        byte[] signature = RLP.encodeElement(preEncodeSignature);

        byte[] encoding = RLP.encodeList(encodedNonce, encodedTo, encodedValue, encodedData, encodedExecutor, signature);

        return prependVersion(encoding);
    }

    /**
     * Returns the transaction hash of the provided signed transaction, if this is a valid signed
     * transaction.
     *
     * It is assumed that {@code signedTransaction} is the output of the
     * {@code buildSignedTransaction()} method.
     *
     * @param signedTransaction A signed transaction.
     * @return the transaction hash of the signed transaction.
     * @throws NullPointerException if signedTransaction is null.
     * @throws IllegalStateException if the provided bytes could not be interpreted.
     */
    public static byte[] getTransactionHashOfSignedTransaction(byte[] signedTransaction) {
        if (signedTransaction == null) {
            throw new NullPointerException("cannot extract hash from a null transaction.");
        }
        if (signedTransaction[0] != 0) {
            throw new IllegalArgumentException("Hashing is only implemented for version code 0");
        }

        return blake2b(signedTransaction);
    }

    /**
     * Resets the builder so that it is in its initial state.
     *
     * The state of the builder after a call to this method is the same as the state of a newly
     * constructed builder.
     */
    public void reset() {
        this.privateKey = null;
        this.nonce = null;
        this.value = null;
        this.destination = null;
        this.data = null;
        this.executor = null;
    }

    private static byte[] addSkPrefix(String skString) {
        if(skString != null && skString.startsWith("0x"))
            skString = skString.substring(2);

        String skEncoded = "302e020100300506032b657004220420" + skString;
        //System.out.println("length: " + hexToBytes(skEncoded).length );
        byte[] bytes = hexToBytes(skEncoded);

        //take 48 byte only.
        if(bytes.length > 48)
            return Arrays.copyOfRange(bytes, 0, 48);
        else
            return bytes;
    }

    private static byte[] blake2b(byte[] msg) {
        Blake2b.Digest digest = Blake2b.Digest.newInstance(32);
        digest.update(msg);
        return digest.digest();
    }

    private static byte[] sign(EdDSAPrivateKey privateKey, byte[] data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
        EdDSAEngine edDSAEngine = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
        edDSAEngine.initSign(privateKey);
        return edDSAEngine.signOneShot(data);
    }

    private static byte[] prependVersion(byte[] encoding) {
        byte[] ret = new byte[encoding.length + 1];
        ret[0] = VERSION;
        System.arraycopy(encoding, 0, ret, 1, encoding.length);
        return ret;
    }
}