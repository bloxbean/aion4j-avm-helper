package org.aion4j.avm.helper.local;

import org.aion.avm.core.AvmConfiguration;
import org.aion.avm.core.CommonAvmFactory;
import org.aion.avm.core.util.ABIUtil;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.core.util.StorageWalker;
import org.aion.avm.tooling.StandardCapabilities;
import org.aion.avm.tooling.abi.ABICompiler;
import org.aion.avm.tooling.deploy.JarOptimizer;
import org.aion.kernel.*;
import org.aion.types.Address;
import org.aion.vm.api.interfaces.IExecutionLog;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.aion.vm.api.interfaces.VirtualMachine;
import org.aion4j.avm.helper.api.CallResponse;
import org.aion4j.avm.helper.api.DeployResponse;
import org.aion4j.avm.helper.exception.CallFailedException;
import org.aion4j.avm.helper.exception.DeploymentFailedException;
import org.aion4j.avm.helper.exception.LocalAVMException;
import org.aion4j.avm.helper.util.HexUtil;
import org.aion4j.avm.helper.util.MethodCallArgsUtil;
import static org.aion4j.avm.helper.util.ConfigUtil.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalAvmNode {
    private Address defaultAddress; // = KernelInterfaceImpl.PREMINED_ADDRESS;
    Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);

    private  VirtualMachine avm;
    private TestingKernel kernel;

    private long energyLimit = 100000000; //TODO Needs to configured by the project
    private long energyPrice = 1L;  //TODO Needs to be configured by the project

    //By default doesn't do abiCompile. The deployed jar should be pre-compiled and pass to deploy.
    private boolean forceAbiCompile = false;

    public LocalAvmNode(String storagePath, String senderAddress) {
        if(storagePath.isEmpty())
            throw new LocalAVMException("Storage path cannot be null for embedded Avm deployment");

        defaultAddress = Address.wrap(Helpers.hexStringToBytes(senderAddress));

        init(storagePath);
    }

    public void init(String storagePath) {
        verifyStorageExists(storagePath);
        File storagePathFile = new File(storagePath);
        kernel = new TestingKernel(storagePathFile);

        //Open account
        if(kernel.getBalance(defaultAddress) == null || kernel.getBalance(defaultAddress) == BigInteger.ZERO) {
            kernel.createAccount(defaultAddress);
            kernel.adjustBalance(defaultAddress, BigInteger.valueOf(100000000000000L));

            System.out.println(String.format("Created default account %s with balance %s", defaultAddress, BigInteger.valueOf(100000000000000L) ));
        }

        AvmConfiguration avmConfiguration = new AvmConfiguration();
        avmConfiguration.enableVerboseConcurrentExecutor=getAvmConfigurationBooleanProps(ENABLE_VERBOSE_CONCURRENT_EXECUTOR, false);
        avmConfiguration.enableVerboseContractErrors=getAvmConfigurationBooleanProps(ENABLE_VERBOSE_CONTRACT_ERRORS, false);
        avmConfiguration.preserveDebuggability=getAvmConfigurationBooleanProps(PRESERVE_DEBUGGABILITY, false);

        avm = CommonAvmFactory.buildAvmInstanceForConfiguration(new StandardCapabilities(), avmConfiguration);
    }

    public DeployResponse deploy(String jarFilePath) throws DeploymentFailedException {
        return deploy(jarFilePath, null, null);
    }

    public DeployResponse deploy(String jarFilePath, String deployArgs, String deployer) throws DeploymentFailedException {

        Address deployerAddress = null;

        if(deployer == null || deployer.isEmpty())
            deployerAddress = defaultAddress;
        else
            deployerAddress = Address.wrap(Helpers.hexStringToBytes(deployer));

        //parse deploy args
        byte[] deployArgsBytes = null;
        if(deployArgs != null && !deployArgs.isEmpty()) {
            try {
                deployArgsBytes = encodeDeployArgsString(deployArgs);
            } catch (CallFailedException e) {
                throw new DeploymentFailedException("Deployment error", e);
            }
        }

        TransactionContext txContext = createDeployTransaction(jarFilePath, deployArgsBytes, deployerAddress, BigInteger.ZERO);

        DeployResponse deployResponse = createDApp(txContext);

        return deployResponse;
    }

    public CallResponse call(String contract, String sender, String method, String argsString, BigInteger value) throws CallFailedException {

        Address contractAddress = Address.wrap(Helpers.hexStringToBytes(contract));

        Address senderAddress = null;

        if(sender == null || sender.isEmpty())
            senderAddress = defaultAddress;
        else
            senderAddress = Address.wrap(Helpers.hexStringToBytes(sender));

        Object[] args = null;
        try {
            args = MethodCallArgsUtil.parseMethodArgs(argsString);
        } catch (Exception e) {
            throw new CallFailedException("Method argument parsing error", e);
        }

        TransactionContext txContext = createCallTransaction(contractAddress, senderAddress, method, args, value, energyLimit, energyPrice);

        TransactionResult result = avm.run(kernel, new TransactionContext[]{txContext})[0].get();

        if(result.getResultCode().isSuccess()) {
            CallResponse response = new CallResponse();

            byte[] retData = result.getReturnData();

            if(retData != null) {
                try {
                    Object retObj = ABIUtil.decodeOneObject(retData);

                    if (retObj != null && retObj instanceof avm.Address) {
                        String addStr = HexUtil.bytesToHexString(((avm.Address) retObj).unwrap());
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

            response.setEnergyUsed(((AvmTransactionResult) result).getEnergyUsed());
            response.setStatusMessage(result.getResultCode().toString());
            printExecutionLog(txContext);

            return response;
        } else {

            byte[] retData = result.getReturnData();
            if(retData != null) {

                String resultData = Helpers.bytesToHexString(retData);
                //failed.
                throw new CallFailedException(String.format("Dapp call failed. Code: %s, Reason: %s",
                        result.getResultCode().toString(), resultData));
            } else {
                throw new CallFailedException(String.format("Dapp call failed. Code: %s, Reason: %s",
                        result.getResultCode().toString(), retData));
            }
        }
    }

    private void printExecutionLog(TransactionContext txContext) {
        //Logs
        List<IExecutionLog> executionLogs = txContext.getSideEffects().getExecutionLogs();
        if(executionLogs != null && executionLogs.size() > 0) {
            System.out.println("************************ Execution Logs ****************************");

            executionLogs.forEach(exLog -> {
                System.out.println("Hex Data: " + HexUtil.bytesToHexString(exLog.getData()));

                if(exLog.getTopics() != null) {
                    List<byte[]> topics = exLog.getTopics();

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

    private DeployResponse createDApp(TransactionContext txContext) throws DeploymentFailedException {

        TransactionResult result = avm.run(kernel, new TransactionContext[] {txContext})[0].get();

        if(result.getResultCode().isSuccess()) {
            DeployResponse deployResponse = new DeployResponse();

            String dappAddress = Helpers.bytesToHexString(result.getReturnData());

            deployResponse.setAddress(dappAddress);
            deployResponse.setEnergyUsed(((AvmTransactionResult) result).getEnergyUsed());
            deployResponse.setStatusMessage(result.getResultCode().toString());

            return deployResponse;
        } else {

            String resultData = Helpers.bytesToHexString(result.getReturnData());
            //failed.
            throw new DeploymentFailedException(String.format("Dapp deployment failed. Code: %s, Reason: %s",
                    result.getResultCode().toString(), resultData));
        }
    }

    private TransactionContext createDeployTransaction(String jarPath, byte[] deployArgs, Address sender, BigInteger value)
            throws DeploymentFailedException {

        Path path = Paths.get(jarPath);
        byte[] jar;
        try {
            jar = Files.readAllBytes(path);
        }catch (IOException e){
            throw new DeploymentFailedException("deploy : Invalid location of Dapp jar - " + jarPath);
        }

        byte[] deployBytes = new CodeAndArguments(jar, deployArgs).encodeToBytes();

        if(this.forceAbiCompile) //do AbiCompile
            deployBytes = compileDappBytes(deployBytes);

        Transaction createTransaction = Transaction.create(sender, kernel.getNonce(sender),
                value, deployBytes, energyLimit, energyPrice);

        return TransactionContextImpl.forExternalTransaction(createTransaction, block);

    }

    private static byte[] compileDappBytes(byte[] dappBytesWithArgs) {
        ABICompiler compiler = new ABICompiler();
        CodeAndArguments oldCodeAndArguments = CodeAndArguments.decodeFromBytes(dappBytesWithArgs);
        compiler.compile(oldCodeAndArguments.code);
        CodeAndArguments newCodeAndArguments = new CodeAndArguments(compiler.getJarFileBytes(),
                oldCodeAndArguments.arguments);
        byte[] deployBytes = newCodeAndArguments.encodeToBytes();

        return deployBytes;
    }

    public TransactionContext createCallTransaction(Address contract, Address sender, String method, Object[] args,
                                                    BigInteger value, long energyLimit, long energyPrice) {

        /*if (contract.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid Dapp address ");
        }

        if (sender.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid sender address");
        }*/

        byte[] arguments = ABIUtil.encodeMethodArguments(method, args);

//        String callData = Helpers.bytesToHexString(arguments);
//        System.out.println("******** Call data: " + callData);
        BigInteger biasedNonce = kernel.getNonce(sender);//.add(BigInteger.valueOf(nonceBias));
        Transaction callTransaction = Transaction.call(sender, contract, biasedNonce, value, arguments, energyLimit, energyPrice);
        return TransactionContextImpl.forExternalTransaction(callTransaction, block);

    }

    public boolean createAccountWithBalance(String address, BigInteger balance) {

        Address account = Address.wrap(Helpers.hexStringToBytes(address));

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

    public BigInteger getBalance(String address) {

        Address account = Address.wrap(Helpers.hexStringToBytes(address));

        BigInteger balance = kernel.getBalance(account);

        if(balance == null)
            return BigInteger.ZERO;
        else
            return balance;
    }

    public void explore(String dappAddress, PrintStream printStream) throws Exception {

        try {
            StorageWalker.walkAllStaticsForDapp(new StandardCapabilities(), printStream, kernel, Address.wrap(HexUtil.hexStringToBytes(dappAddress)));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to explore storage for dApp : " + dappAddress, ex);
        }
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

    //Called for remote
    public static String getBytesForDeploy(String dappJarPath, String deployArgsStr) throws CallFailedException {
        try {
            Path path = Paths.get(dappJarPath);
            byte[] jar = Files.readAllBytes(path);

            byte[] deployArgsBytes = null;
            if(deployArgsStr != null && !deployArgsStr.isEmpty())
                deployArgsBytes = encodeDeployArgsString(deployArgsStr);

            if(deployArgsBytes == null) deployArgsBytes = new byte[0];

            return Helpers.bytesToHexString(
                    compileDappBytes(new CodeAndArguments(jar, deployArgsBytes).encodeToBytes()));
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
                    return HexUtil.bytesToHexString(((avm.Address) result).unwrap());
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
        ABICompiler compiler = new ABICompiler();
        compiler.compile(jarBytes);

        return compiler.getJarFileBytes();
    }

    public static byte[] optimizeJarBytes(byte[] jarBytes, boolean debugMode) {
        JarOptimizer jarOptimizer = new JarOptimizer(debugMode);
        return jarOptimizer.optimize(jarBytes);
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