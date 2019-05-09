package org.aion4j.avm.helper.crypto;

/**
 *
 * @Author Satya
 */
public class AccountKey {

    private byte[] privateKeyBytes;
    private byte[] publicKeyBytes;

    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    public void setPrivateKeyBytes(byte[] privateKeyBytes) {
        this.privateKeyBytes = privateKeyBytes;
    }

    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    public void setPublicKeyBytes(byte[] publicKeyBytes) {
        this.publicKeyBytes = publicKeyBytes;
    }
}
