package org.aion4j.avm.helper.local;

import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion4j.avm.helper.api.CallResponse;
import org.aion4j.avm.helper.api.DeployResponse;
import org.aion4j.avm.helper.exception.CallFailedException;
import org.aion4j.avm.helper.exception.DeploymentFailedException;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LocalAvmNodeTest {

    private static String defaultAddress = "0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca";
    private static File testDataFolder;
    private static LocalAvmNode localAvmNode;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setupClass() throws Exception {
        File distJar = new File(LocalAvmNodeTest.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());

        testDataFolder = new File(distJar.getParent() + File.separator + "test-data");

        if(!testDataFolder.exists())
            testDataFolder.mkdirs();

        testDataFolder.deleteOnExit();

        localAvmNode = new LocalAvmNode(testDataFolder + File.separator + "storage", defaultAddress);
    }

    @Before
    public void setup() throws URISyntaxException {

    }

    @AfterClass
    public static void tearDownClass() {
        if(testDataFolder != null)
            deleteDirectory(testDataFolder);
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
    public void compileJarBytesAndWriteAbiTest() throws Exception {
        InputStream in = this.getClass().getResourceAsStream("/TestContract.jar");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        byte[] jarBytes = os.toByteArray();

        Path abiFile = Files.createTempFile("avm", ".abi");

        FileOutputStream fout = new FileOutputStream(abiFile.toFile());
        abiFile.toFile().deleteOnExit();

        byte[] compiledBytes = LocalAvmNode.compileJarBytesAndWriteAbi(jarBytes, fout);

        System.out.println(abiFile.toAbsolutePath());

        assertNotNull(compiledBytes);
        assertTrue(compiledBytes.length > 0);
        assertTrue(abiFile.toFile().exists());

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

    @Test
    public void deployTest() throws Exception {
        DeployResponse deployResponse = deployTestContract(localAvmNode, "TestContract.jar", null, null);
        String contractAddress = deployResponse.getAddress();

        assertTrue(deployResponse.isSuccess());
    }

    @Test
    public void deployTestWithValue() throws Exception {
        DeployResponse deployResponse = deployTestContract(localAvmNode, "TestContract.jar", null, null, new BigInteger("670000000000"));
        String contractAddress = deployResponse.getAddress();

        BigInteger contractBalance = localAvmNode.getBalance(contractAddress);

        assertTrue(deployResponse.isSuccess());
        assertEquals(new BigInteger("670000000000"), contractBalance);
    }

    @Test
    public void deployFailedTest() throws Exception {
        thrown.expect(DeploymentFailedException.class);
        thrown.expectMessage(containsString("insufficient balance"));

        String deployerWithInsufficientBalance = "0xa5555e8793a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b29999";
        DeployResponse deployResponse = deployTestContract(localAvmNode, "TestContract.jar", null, deployerWithInsufficientBalance);
        String contractAddress = deployResponse.getAddress();

        assertFalse(deployResponse.isSuccess());
    }


    @Test
    public void deployTestContractAndSetAndGetCall() throws Exception {

        DeployResponse deployResponse = deployTestContract(localAvmNode, "TestContract.jar", null, null);
        String contractAddress = deployResponse.getAddress();

        String contentToSet = System.currentTimeMillis() + "";

        CallResponse callResponse = localAvmNode.call(contractAddress, defaultAddress, "setString", "-T " + contentToSet, BigInteger.valueOf(50));
        assertTrue("Set call status true", callResponse.isSuccess());

        BigInteger contractBalance = localAvmNode.getBalance(contractAddress);
        assertThat(contractBalance, equalTo(BigInteger.valueOf(50)));

        CallResponse getCallResponse = localAvmNode.call(contractAddress, defaultAddress, "getString", null, BigInteger.ZERO);
        assertTrue("Get call status true", getCallResponse.isSuccess());
        assertThat(getCallResponse.getData(), is(contentToSet));
    }

    @Test
    public void deployTestContractAndCallMethodWithInsufficientBalance() throws Exception {

        DeployResponse deployResponse = deployTestContract(localAvmNode, "TestContract.jar", null, null);
        String contractAddress = deployResponse.getAddress();

        String caller ="0xa0115e8793a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b24397";

        String contentToSet = System.currentTimeMillis() + "";

        thrown.expect(CallFailedException.class);
        thrown.expectMessage(containsString("insufficient balance"));

        CallResponse callResponse = localAvmNode.call(contractAddress, caller, "setString", "-T " + contentToSet, BigInteger.valueOf(50));
        assertFalse("Set call status false", callResponse.isSuccess());

    }

    @Test
    public void transferTest() throws Exception {

        String toAddress = "0xa0115e8793a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b24396";
        BigInteger initialBalance = new BigInteger("40000000000");

        localAvmNode.createAccountWithBalance(toAddress, initialBalance);

        assertEquals(initialBalance, localAvmNode.getBalance(toAddress));

        BigInteger transferAmt = new BigInteger("500");
        localAvmNode.transfer(toAddress, transferAmt);
        System.out.println(localAvmNode.getBalance(toAddress));

        assertEquals(initialBalance.add(transferAmt), localAvmNode.getBalance(toAddress));
    }

    @Test
    public void tranferTestWithoutAccountCreate() {
        String toAddress = "0xa0885e8793a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b24396";

        BigInteger transferAmt = new BigInteger("500");
        localAvmNode.transfer(toAddress, transferAmt);
        System.out.println(localAvmNode.getBalance(toAddress));

        assertEquals(transferAmt, localAvmNode.getBalance(toAddress));
    }

    private DeployResponse deployTestContract(LocalAvmNode localAvmNode, String jar, String deployArgs, String deployer) throws Exception {
        return deployTestContract(localAvmNode, jar, deployArgs, deployer, null);
    }

    private DeployResponse deployTestContract(LocalAvmNode localAvmNode, String jar, String deployArgs, String deployer, BigInteger value) throws Exception {
        InputStream in = this.getClass().getResourceAsStream("/" + jar);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        byte[] jarBytes = os.toByteArray();

        byte[] optimizeJarBytes = LocalAvmNode.optimizeJarBytes(jarBytes, false);

        File distJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation()
                .toURI());

        String compiledJar = testDataFolder.getAbsolutePath() + File.separator + jar;

        writeByte(compiledJar, optimizeJarBytes);

        System.out.println("Dist folder >> " + distJar.getParent());

        if (deployer == null)
            deployer = defaultAddress;

        DeployResponse deployResponse = localAvmNode.deploy(compiledJar, deployArgs, deployer, value);

        return deployResponse;
    }

    @Test
    public void createAccountWithBalanceTest() {

        String address = "a0458c209555006804064bd488c16d5219179c90c3955caa04e3a1102f3ce7b2";
        BigInteger balance = BigInteger.valueOf(500).multiply(BigInteger.valueOf(1000000000000000000L));
        localAvmNode.createAccountWithBalance(address, balance);

        BigInteger currBalance = localAvmNode.getBalance(address);

        System.out.println("Expected Balance >>> " + balance.toString());
        System.out.println("Current Balance >>> " + currBalance.toString());

        assertThat(currBalance, equalTo(balance));
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

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}