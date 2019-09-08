package org.aion4j.avm.helper.local;

import org.aion.avm.core.*;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.embed.StandardCapabilities;
import org.aion.avm.tooling.ABIUtil;
import org.aion.avm.tooling.abi.ABICompiler;
import org.aion.avm.tooling.deploy.OptimizedJarBuilder;
import org.aion.avm.userlib.CodeAndArguments;
import org.aion.kernel.TestingBlock;
import org.aion.kernel.TestingState;
import org.aion.types.AionAddress;
import org.aion.types.Log;
import org.aion.types.Transaction;
import org.aion.types.TransactionResult;
import org.aion4j.avm.helper.api.CallResponse;
import org.aion4j.avm.helper.api.DeployResponse;
import org.aion4j.avm.helper.exception.CallFailedException;
import org.aion4j.avm.helper.exception.DeploymentFailedException;
import org.aion4j.avm.helper.exception.LocalAVMException;
import org.aion4j.avm.helper.util.HexUtil;
import org.aion4j.avm.helper.util.MethodCallArgsUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.aion4j.avm.helper.util.ConfigUtil.*;

public class LocalAvmNode {
    private static final BigInteger ONE_AION = new BigInteger("1000000000000000000"); //1 Aion
    public static final int ABI_COMPILER_VERSION = 1;
    private AionAddress defaultAddress; // = KernelInterfaceImpl.PREMINED_ADDRESS;
    TestingBlock block = new TestingBlock(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);

    private AvmImpl avm;
    private TestingState kernel;

    private long energyLimit = 100000000; //TODO Needs to configured by the project
    private long energyPrice = 1L;  //TODO Needs to be configured by the project
    //By default doesn't do abiCompile. The deployed jar should be pre-compiled and pass to deploy.
    private boolean forceAbiCompile = false;
    private boolean preserveDebuggability = false;

    public LocalAvmNode(String storagePath, String senderAddress) {
        if(storagePath.isEmpty())
            throw new LocalAVMException("Storage path cannot be null for embedded Avm deployment");

        defaultAddress = new AionAddress(Helpers.hexStringToBytes(senderAddress));

        init(storagePath);
    }

    public void init(String storagePath) {
        verifyStorageExists(storagePath);
        File storagePathFile = new File(storagePath);
        kernel = new TestingState(storagePathFile, block);

        //Open account
        if(kernel.getBalance(defaultAddress) == null || kernel.getBalance(defaultAddress) == BigInteger.ZERO) {
            BigInteger initialBalance = ONE_AION.multiply(new BigInteger("100000")); //100,000 Aion

            kernel.createAccount(defaultAddress);
            kernel.adjustBalance(defaultAddress, initialBalance);

            System.out.println(String.format("Created default account %s with balance %s", defaultAddress, initialBalance ));
        }

        AvmConfiguration avmConfiguration = new AvmConfiguration();
        avmConfiguration.enableVerboseConcurrentExecutor=getAvmConfigurationBooleanProps(ENABLE_VERBOSE_CONCURRENT_EXECUTOR, false);
        avmConfiguration.enableVerboseContractErrors=getAvmConfigurationBooleanProps(ENABLE_VERBOSE_CONTRACT_ERRORS, false);
        avmConfiguration.preserveDebuggability=getAvmConfigurationBooleanProps(PRESERVE_DEBUGGABILITY, false);

        preserveDebuggability = avmConfiguration.preserveDebuggability;

        avm = CommonAvmFactory.buildAvmInstanceForConfiguration(new StandardCapabilities(), avmConfiguration);
    }

    public DeployResponse deploy(String jarFilePath) throws DeploymentFailedException {
        return deploy(jarFilePath, null, null, BigInteger.ZERO);
    }

    public DeployResponse deploy(String jarFilePath, String deployArgs, String deployer) throws DeploymentFailedException {
        return deploy(jarFilePath, deployArgs, deployer, BigInteger.ZERO);
    }

    public DeployResponse deploy(String jarFilePath, String deployArgs, String deployer, BigInteger value) throws DeploymentFailedException {

        AionAddress deployerAddress = null;

        if(deployer == null || deployer.isEmpty())
            deployerAddress = defaultAddress;
        else
            deployerAddress = new AionAddress(Helpers.hexStringToBytes(deployer));

        //parse deploy args
        byte[] deployArgsBytes = null;
        if(deployArgs != null && !deployArgs.isEmpty()) {
            try {
                deployArgsBytes = encodeDeployArgsString(deployArgs);
            } catch (CallFailedException e) {
                throw new DeploymentFailedException("Deployment error", e);
            }
        }

        if(value == null)
            value = BigInteger.ZERO;

        Transaction txContext = createDeployTransaction(jarFilePath, deployArgsBytes, deployerAddress, value);

        DeployResponse deployResponse = createDApp(txContext);

        return deployResponse;
    }

    public CallResponse call(String contract, String sender, String method, String argsString, BigInteger value) throws CallFailedException {

        AionAddress contractAddress = new AionAddress(Helpers.hexStringToBytes(contract));

        AionAddress senderAddress = null;

        if(sender == null || sender.isEmpty())
            senderAddress = defaultAddress;
        else
            senderAddress = new AionAddress(Helpers.hexStringToBytes(sender));

        Object[] args = null;
        try {
            args = MethodCallArgsUtil.parseMethodArgs(argsString);
        } catch (Exception e) {
            throw new CallFailedException("Method argument parsing error", e);
        }

        Transaction txContext = createCallTransaction(contractAddress, senderAddress, method, args, value, energyLimit, energyPrice);

        //generate block
        this.kernel.generateBlock();

        TransactionResult result = avm.run(kernel, new Transaction[]{txContext}, ExecutionType.ASSUME_MAINCHAIN,
                kernel.getBlockNumber()-1)[0].getResult();

        if(result.transactionStatus.isSuccess()) {
            CallResponse response = new CallResponse();

            byte[] retData = result.copyOfTransactionOutput().orElse(new byte[0]);

            if(retData != null) {
                try {
                    Object retObj = ABIUtil.decodeOneObject(retData);

                    if (retObj != null && retObj instanceof avm.Address) {
                        String addStr = HexUtil.bytesToHexString(((avm.Address) retObj).toByteArray());
                        response.setData(addStr);
                    } else if(retObj != null && is2DArray(retObj)) {
                        String finalRet = MethodCallArgsUtil.print2DArray(retObj);
                        if(finalRet == null)
                            response.setData(HexUtil.bytesToHexString(retData));
                        else
                            response.setData(finalRet);
                    } else if(retObj != null && isArray(retObj)) {
                        String finalRet = MethodCallArgsUtil.printArray(retObj);
                        if(finalRet == null)
                            response.setData(HexUtil.bytesToHexString(retData));
                        else
                            response.setData(finalRet);
                    } else {
                        response.setData(retObj);
                    }
                } catch (Exception e) {
                    response.setData(HexUtil.bytesToHexString(retData));
                }
            } else {
                response.setData(null);
            }

            response.setEnergyUsed(result.energyUsed);
            response.setSuccess(true);
            response.setStatusMessage(result.transactionStatus.toString());
            printExecutionLog(result.logs);

            return response;
        } else {

            byte[] retData = result.copyOfTransactionOutput().orElse(new byte[0]);
            if(retData != null) {

                String resultData = Helpers.bytesToHexString(retData);
                //failed.
                throw new CallFailedException(String.format("Contract call failed. Cause: %s, Output: %s",
                        result.transactionStatus.causeOfError, resultData));
            } else {
                throw new CallFailedException(String.format("Contract call failed. Cause: %s, Output: %s",
                        result.transactionStatus.causeOfError, retData));
            }
        }
    }

    private void printExecutionLog(List<Log> executionLogs) {
        //Logs

        if(executionLogs != null && executionLogs.size() > 0) {
            System.out.println("************************ Execution Logs ****************************");

            executionLogs.forEach(exLog -> {
                System.out.println("Hex Data: " + HexUtil.bytesToHexString(exLog.copyOfData()));

                if(exLog.copyOfTopics() != null) {
                    List<byte[]> topics = exLog.copyOfTopics();

                    if(topics != null) {
                        for(byte[] topic: topics) {
                            System.out.println("Topic: " + HexUtil.bytesToHexString(topic));
                        }
                    }
                }
                System.out.println("  ");
            });

            System.out.println("************************ Execution Logs ****************************\n");
        }
    }

    private DeployResponse createDApp(Transaction txContext) throws DeploymentFailedException {

        //generate block
        this.kernel.generateBlock();

        TransactionResult result = avm.run(kernel, new Transaction[] {txContext}, ExecutionType.ASSUME_MAINCHAIN,
                kernel.getBlockNumber()-1)[0].getResult();

        if(result.transactionStatus.isSuccess()) {
            DeployResponse deployResponse = new DeployResponse();

            String dappAddress = Helpers.bytesToHexString(result.copyOfTransactionOutput().orElse(new byte[0]));

            deployResponse.setAddress(dappAddress);
            deployResponse.setEnergyUsed(result.energyUsed);
            deployResponse.setSuccess(true);
            deployResponse.setStatusMessage(result.transactionStatus.toString());

            return deployResponse;
        } else {

            String resultData = Helpers.bytesToHexString(result.copyOfTransactionOutput().orElse(new byte[0]));
            //failed.
            throw new DeploymentFailedException(String.format("Contract deployment failed. Cause: %s, Output: %s",
                    result.transactionStatus.causeOfError, resultData));
        }
    }

    private Transaction createDeployTransaction(String jarPath, byte[] deployArgs, AionAddress sender, BigInteger value)
            throws DeploymentFailedException {

        Path path = Paths.get(jarPath);
        byte[] jar;
        try {
            jar = Files.readAllBytes(path);
        }catch (IOException e){
            throw new DeploymentFailedException("deploy : Invalid location of contract jar - " + jarPath);
        }

        byte[] deployBytes = new CodeAndArguments(jar, deployArgs).encodeToBytes();

        if(this.forceAbiCompile) //do AbiCompile if forceAbiCompile is set. Usually you don't need to do that.
            deployBytes = compileDappBytes(deployBytes, preserveDebuggability);

        Transaction createTransaction = AvmTransactionUtil.create(sender, kernel.getNonce(sender),
                value, deployBytes, energyLimit, energyPrice);

        return createTransaction;

    }

    private static byte[] compileDappBytes(byte[] dappBytesWithArgs, boolean isDebugMode) {
        CodeAndArguments oldCodeAndArguments = CodeAndArguments.decodeFromBytes(dappBytesWithArgs);
        byte[] optimizedDappBytes = new OptimizedJarBuilder(isDebugMode, oldCodeAndArguments.code, ABI_COMPILER_VERSION)
                .withUnreachableMethodRemover()
                .withRenamer()
                .withConstantRemover()
                .getOptimizedBytes();

        CodeAndArguments newCodeAndArguments = new CodeAndArguments(optimizedDappBytes,
                oldCodeAndArguments.arguments);
        byte[] deployBytes = newCodeAndArguments.encodeToBytes();

        return deployBytes;
    }

    public Transaction createCallTransaction(AionAddress contract, AionAddress sender, String method, Object[] args,
                                                    BigInteger value, long energyLimit, long energyPrice) {

        byte[] arguments = ABIUtil.encodeMethodArguments(method, args);

        BigInteger biasedNonce = kernel.getNonce(sender);
        Transaction callTransaction = AvmTransactionUtil.call(sender, contract, biasedNonce, value, arguments, energyLimit, energyPrice);
        return callTransaction;
    }

    public boolean createAccountWithBalance(String address, BigInteger balance) {

        AionAddress account = new AionAddress(Helpers.hexStringToBytes(address));

        //Open account
        if(kernel.getBalance(account) == null || kernel.getBalance(account) == BigInteger.ZERO) {
            kernel.createAccount(account);
            kernel.adjustBalance(account, balance);

            System.out.println(String.format("Create account %s with balance %d", address, balance.longValue()));
            return true;
        } else {
            System.out.println("Account already exists");
            return false;
        }
    }

    /**
     * Transfer amount to the target address
     * @param toAddress
     * @param amount
     * @return
     */
    public boolean transfer(String toAddress, BigInteger amount) {

        AionAddress account = new AionAddress(Helpers.hexStringToBytes(toAddress));

        if(kernel.getBalance(account) == null) {
            kernel.createAccount(account);
        }

        kernel.adjustBalance(account, amount);
        return true;
    }

    public BigInteger getBalance(String address) {

        AionAddress account = new AionAddress(Helpers.hexStringToBytes(address));

        BigInteger balance = kernel.getBalance(account);

        if(balance == null)
            return BigInteger.ZERO;
        else
            return balance;
    }

    public void explore(String dappAddress, PrintStream printStream) throws Exception {

        throw new UnsupportedOperationException("Explorer command is no longer supported.");
    }

    /**
     * Enforce abiCompile during deploy
     * @param flag
     */
    public void setForceAbiCompile(boolean flag) {
        this.forceAbiCompile = flag;
    }

    //Encode deployment args from command line str to byte[]. Needed during deployment.
    public static byte[] encodeDeployArgsString(String deployArgs) throws CallFailedException {
        Object[] args = null;

        try {
            args = MethodCallArgsUtil.parseMethodArgs(deployArgs);
        } catch (Exception e) {
            throw new CallFailedException("Deploy arument parsing error", e);
        }

        if(args == null) {
            System.out.println("Not able to encode deploy args properly");
            return null;
        }

        return ABIUtil.encodeDeploymentArguments(args);
    }

    //Called for remote to deploy a pre-compiled/final jar
    public static String getBytesForDeploy(String dappJarPath, String deployArgsStr) throws CallFailedException {
        try {
            Path path = Paths.get(dappJarPath);
            byte[] jar = Files.readAllBytes(path);

            byte[] deployArgsBytes = null;
            if(deployArgsStr != null && !deployArgsStr.isEmpty())
                deployArgsBytes = encodeDeployArgsString(deployArgsStr);

            if(deployArgsBytes == null) deployArgsBytes = new byte[0];

            return Helpers.bytesToHexString(new CodeAndArguments(jar, deployArgsBytes).encodeToBytes());
        } catch (IOException e) {
            System.out.println(e.toString());
            return null;
        }
    }

    //called from remote Impl
    public static String encodeMethodCallWithArgsString(String method, String methodArgs) throws CallFailedException {

        Object[] args = null;
        try {
            args = MethodCallArgsUtil.parseMethodArgs(methodArgs);
        } catch (Exception e) {
            throw new CallFailedException("Method argument parsing error", e);
        }

        return encodeMethodCall(method, args);
    }

    //called from remote impl to encode method call args
    public static String encodeMethodCall(String method, Object[] args) {
        return Helpers.bytesToHexString(ABIUtil.encodeMethodArguments(method, args));
    }

    //called for remote impl to decode hexstring to object
    public static Object decodeResult(String hex) {
        try {

            Object result = ABIUtil.decodeOneObject(HexUtil.hexStringToBytes(hex));
            if(result != null) {

                if (result instanceof avm.Address) {
                    return HexUtil.bytesToHexString(((avm.Address) result).toByteArray());
                } else if(result != null && is2DArray(result)) {
                    String finalRet = MethodCallArgsUtil.print2DArray(result);
                    if(finalRet == null)
                        return hex;
                    else
                        return finalRet;
                } else if(result != null && isArray(result)) {
                    String finalRet = MethodCallArgsUtil.printArray(result);
                    if(finalRet == null)
                        return hex;
                    else
                        return finalRet;
                } else
                    return result.toString();

            } else
                return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Called mainly from maven goal to compile a Jar content with ABICompiler, mainly to process annotations.
     * @param jarBytes
     * @return
     */
    public static byte[] compileJarBytes(byte[] jarBytes) {
        ABICompiler compiler = ABICompiler.compileJarBytes(jarBytes, ABI_COMPILER_VERSION);

        return compiler.getJarFileBytes();
    }

    /**
     * Called mainly from maven goal to compile a Jar content with ABICompiler, mainly to process annotations and write abi
     * @param jarBytes
     * @return
     */
    public static byte[] compileJarBytesAndWriteAbi(byte[] jarBytes, OutputStream output) {
        ABICompiler compiler = ABICompiler.compileJarBytes(jarBytes, ABI_COMPILER_VERSION);
        byte[] compiledBytes = compiler.getJarFileBytes();

        /** Write abi file **/
        try {
            compiler.writeAbi(output, ABI_COMPILER_VERSION);
        } catch (Exception e) {
            System.out.println("Unable to write abi file to the filesystem. " + e.getMessage());
        } finally {
            if(output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }

        return compiledBytes;
    }

    public static byte[] optimizeJarBytes(byte[] jarBytes, boolean debugMode) {
        byte[] optimizedJar = new OptimizedJarBuilder(debugMode, jarBytes, ABI_COMPILER_VERSION)
                .withUnreachableMethodRemover()
                .withRenamer()
                .withConstantRemover()
                .getOptimizedBytes();

        return optimizedJar;
    }

    private static void verifyStorageExists(String storageRoot) {
        File directory = new File(storageRoot);
        if (!directory.isDirectory()) {
            boolean didCreate = directory.mkdirs();
            // Is this the best way to handle this failure?
            if (!didCreate) {
                throw new LocalAVMException("Unable create storage folder");
            }
        }
    }

    public void shutdown() {
        avm.shutdown();
    }

    private static boolean isArray(Object obj)
    {
        return obj!=null && obj.getClass().isArray();
    }

    private static boolean is2DArray(Object obj)
    {
        return obj!=null && obj.getClass().getTypeName().endsWith("[][]");
    }

}