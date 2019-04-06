package org.aion4j.avm.helper.local;

import org.aion.avm.core.util.ABIUtil;
import org.aion4j.avm.helper.api.CallResponse;
import org.aion4j.avm.helper.api.DeployResponse;
import org.aion4j.avm.helper.exception.CallFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class LocalAvmNodeTest {

    private String defaultAddress = "0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca";
    private File testDataFolder;

    @Before
    public void setup() throws URISyntaxException {
        File distJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation()
                .toURI());

        testDataFolder = new File(distJar.getParent() + File.separator + "test-data");

        if(!testDataFolder.exists())
            testDataFolder.mkdirs();

        testDataFolder.deleteOnExit();
    }

    @After
    public void tearDown() {
        if(testDataFolder != null)
            testDataFolder.delete();
    }

    @Test
    public void encodeDeployArgsStringMultiArgs() throws CallFailedException {

       /* String cmdArgs = "-T John -I 5 -I 9 -T Harry -A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea";

        byte[] bytes = LocalAvmNode.encodeDeployArgsString(cmdArgs);

        Object decoded = ABIDecoder.decodeOneObject(bytes);*/

    }

    @Test
    public void encodeDeployArgsStringSingleArgs() throws CallFailedException {
        String cmdArgs = "-T John";

        byte[] bytes = LocalAvmNode.encodeDeployArgsString(cmdArgs);
        Object decoded = ABIUtil.decodeOneObject(bytes);
        System.out.println(decoded);

        assertEquals("John", decoded);
    }

    @Test
    public void encodeDeployArgsStringSingleArgsInt() throws CallFailedException {
        String cmdArgs = "-I 8000";

        byte[] bytes = LocalAvmNode.encodeDeployArgsString(cmdArgs);
        Object decoded = ABIUtil.decodeOneObject(bytes);
        System.out.println(decoded);

        assertEquals(8000, decoded);
    }

    @Test
    public void compileJarBytesTest() throws Exception {
        InputStream in = this.getClass().getResourceAsStream("/TestContract.jar");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        byte[] jarBytes = os.toByteArray();

        byte[] compiledBytes = LocalAvmNode.compileJarBytes(jarBytes);

        assertNotNull(compiledBytes);
        assertTrue(compiledBytes.length > 0);

    }

    @Test
    public void compileJarAndDeployTest() throws Exception {
        InputStream in = this.getClass().getResourceAsStream("/TestContract.jar");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        byte[] jarBytes = os.toByteArray();

        byte[] compiledBytes = LocalAvmNode.compileJarBytes(jarBytes);

        File distJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation()
                .toURI());

        String compiledJar = testDataFolder.getAbsolutePath() + File.separator + "TestContract.jar";

        writeByte(compiledJar, compiledBytes);

        System.out.println("Dist folder >> " + distJar.getParent());

        LocalAvmNode localAvmNode = new LocalAvmNode(testDataFolder + File.separator + "storage", defaultAddress);
       // localAvmNode.setForceAbiCompile(true);
        DeployResponse deployResponse = localAvmNode.deploy(compiledJar);

        String contractAddress = deployResponse.getAddress();

        assertNotNull(contractAddress);

        CallResponse callResponse = localAvmNode.call(contractAddress, defaultAddress, "greet", "-T AVM", BigInteger.ZERO);

        assertTrue(callResponse.getData().toString().contains("AVM"));
        System.out.println(" Response >>>> " + callResponse.getData());

        assertNotNull(compiledBytes);
        assertTrue(compiledBytes.length > 0);

    }

    private static void writeByte(String file, byte[] bytes)
    {
        try {

            // Initialize a pointer
            // in file using OutputStream
            OutputStream
                    os
                    = new FileOutputStream(file);

            // Starts writing the bytes in it
            os.write(bytes);
            // Close the file
            os.close();
        }

        catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}