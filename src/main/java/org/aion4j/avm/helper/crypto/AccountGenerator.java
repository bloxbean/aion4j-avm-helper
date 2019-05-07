package org.aion4j.avm.helper.crypto;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import org.aion.base.util.ByteUtil;
import org.aion4j.avm.helper.signing.Blake2b;
import org.aion4j.avm.helper.util.HexUtil;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import static net.i2p.crypto.eddsa.Utils.hexToBytes;

public class AccountGenerator {

    private static final EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    private static final byte A0_IDENTIFIER = ByteUtil.hexStringToBytes("0xA0")[0];

    public void newAddress() throws InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchAlgorithmException {

        SecureRandom secureRandom = new SecureRandom();
        byte[] seedBytes = new byte[64];
        secureRandom.nextBytes(seedBytes);
        System.out.println(">> " + ByteUtil.toHexString(seedBytes));

        EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(addSkPrefix(ByteUtil.toHexString(seedBytes))));
        byte[] address = computeA0Address(privateKey.getAbyte());

        System.out.println(HexUtil.bytesToHexString(address));
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

    public static byte[] computeA0Address(byte[] publicKey) {
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.put(A0_IDENTIFIER);
        // [1:]
        buf.put(blake2b(publicKey), 1, 31);
        return buf.array();
    }

    public static void main(String[] args) throws InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchAlgorithmException {
        AccountGenerator generator = new AccountGenerator();
        generator.newAddress();
    }

}