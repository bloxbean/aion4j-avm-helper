package org.aion4j.avm.helper.faucet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.nettgryppa.security.HashCash;
import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.api.logs.ErrorDelegateLog;
import org.aion4j.avm.helper.api.logs.Slf4jLog;
import org.aion4j.avm.helper.exception.AVMRuntimeException;
import org.aion4j.avm.helper.exception.RemoteAvmCallException;
import org.aion4j.avm.helper.faucet.model.Challenge;
import org.aion4j.avm.helper.faucet.model.Network;
import org.aion4j.avm.helper.faucet.model.TopupResult;
import org.aion4j.avm.helper.remote.RemoteAVMNode;
import org.aion4j.avm.helper.remote.RemoteAvmAdapter;
import org.aion4j.avm.helper.util.CryptoUtil;
import org.aion4j.avm.helper.util.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaucetService {
    private final ClassLoader avmJarClassLoader;
    private String nodeUrl;
    private String faucetWebUrl;
    private String faucetContractAddress;
    private String networkId;
    private Log log;
    private RemoteAvmAdapter remoteAvmAdapter;

    private long defaultGas;
    private long defaultGasPrice;

    public FaucetService(ClassLoader avmJarClassLoader, String nodeUrl, String faucetWebUrl, String faucetContractAddress, Log _log) {
        this.avmJarClassLoader = avmJarClassLoader;
        this.nodeUrl = nodeUrl;
        this.faucetWebUrl = faucetWebUrl;
        this.log = _log;

        //In normal cases, faucetContractAddress is passed as null
        Network network = resolveNetwork(nodeUrl, faucetContractAddress);
        this.faucetContractAddress = network != null ? network.getFaucetContract() : null;
        this.networkId = network != null ? network.getId() : null;

        if(this.log == null) {
            this.log = new Slf4jLog(LoggerFactory.getLogger(FaucetService.class));
        }

        log.info("Network                 : " + network.getNetwork());
        log.info("Faucet contract address : " + network.getFaucetContract());
        log.info("");

        if(StringUtils.isEmpty(this.faucetContractAddress)) {
            throw new AVMRuntimeException(String.format("Faucet contract address is not set for network : %s ", network.getNetwork()));
        }

        Log remoteAdapterLog = _log != null && !_log.isDebugEnabled() ? new ErrorDelegateLog(this.log) : this.log;

        this.remoteAvmAdapter = new RemoteAvmAdapter(nodeUrl, remoteAdapterLog);

        Unirest.setObjectMapper(new ObjectMapper() {
            com.fasterxml.jackson.databind.ObjectMapper mapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public String writeValue(Object value) {
                try {
                    return mapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }

            }

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return mapper.readValue(value, valueType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    private Network resolveNetwork(String nodeUrl, String faucetContractAddress) {
        if(!StringUtils.isEmpty(faucetContractAddress)) //Custom contract address provided by user
            return new Network(null, null, null, faucetContractAddress);

        NetworkHelper networkHelper = new NetworkHelper(log);
        Network network = networkHelper.getNetworkFromWeb3RpcUrl(nodeUrl);

        return network;
    }

    public void setDefaultGas(long defaultGas) {
        this.defaultGas = defaultGas;
    }

    public void setDefaultGasPrice(long defaultGasPrice) {
        this.defaultGasPrice = defaultGasPrice;
    }

    public void topup(String account, String accountPk) throws RemoteAvmCallException {

        boolean isFaucetWebCall = isFacetWebCallRequired(remoteAvmAdapter, account);

        if(isFaucetWebCall) { //Make faucet web call for new account
            log.info("Let's register the address and get some minimum AION coins through Faucet Web");
            //TODO retry

            allocateInitialBalanceThroughFaucetWeb(account);
        }

        //Invoke
        log.info("Let's get some coin from the Faucet contract");

        try {
            invokeContractForBalanceTopup(accountPk, account);
        } catch (Exception e) {
            log.debug("Account topup failed", e);
            throw new RemoteAvmCallException("Account topup failed", e);
        }

        BigInteger balance = remoteAvmAdapter.getBalance(account);

        if(balance == null || BigInteger.ZERO.equals(balance)) {
            log.error("Could not send some initial AION coins to the address");
            throw new RemoteAvmCallException("Topup registration failed for address : " + account);
        }

        BigDecimal aionBalance = CryptoUtil.ampToAion(balance);

        log.info("Account           : " + account);
        log.info(String.format("New balance (nAmp): %s (%s Aion)", balance, String.format("%.12f",aionBalance)));

        return;
    }

    private void allocateInitialBalanceThroughFaucetWeb(String account) throws RemoteAvmCallException {
        //Get challenge from Faucet server
        Challenge challenge = null;
        try {
            log.info("Fetching challenge from the Faucet web server ....");
            challenge = getChallenge();
        } catch (UnirestException ex) {
            log
                    .error(String.format("Get challenge failed"),
                            ex);
            throw new RemoteAvmCallException("Get challenge failed", ex);
        }

        if(challenge == null)
            throw new RemoteAvmCallException("Get challenge failed");

        Map<String, List<String>> extensions = new HashMap<>();
        List<String> extensionList = new ArrayList<>();
        extensionList.add(String.valueOf(challenge.getCounter()));
        extensionList.add(account);
        if(!StringUtils.isEmpty(this.networkId))
            extensionList.add(this.networkId);

        //add counter to extension
        extensions.put("data", extensionList);

        if(log.isDebugEnabled())
            log.debug("Extensions >>>>>>> " + extensionList);

        log.info("Start genereting proof with value " + challenge.getValue());

        if(challenge.getValue() > 30) {
            log.info("You may be making too many requests to the faucet. " +
                    "Your challenge is too high. It might take little longer to generate the proof.");
        }

        //Start minting hashcash
        long t1 = System.currentTimeMillis();
        HashCash cash = null;
        try {
            cash = HashCash.mintCash(challenge.getMessage(), extensions, challenge.getValue(), 1);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating proof",e);
            throw new RemoteAvmCallException("Error generating proof");
        }

        if(cash == null) {
            throw new RemoteAvmCallException("Error generating proof");
        }

        long t2 = System.currentTimeMillis();

        log.info("Time spent in minting proof : " + (t2 - t1) / 1000 + "sec");

        log.info("Send hascash proof to server : " + cash.toString());

        TopupResult topupResult = null;
        try {
            topupResult = submitHashCash(account, cash);
            if(topupResult != null) {
                log.info("Register result >> " + topupResult);
            } else {
                log.error("Account could not be credited");
                throw new RemoteAvmCallException("Error in crediting account");
            }
        } catch (UnirestException e) {
            log.error("Topup failed for address : " + account,e);

            throw new RemoteAvmCallException("Topup failed for address : " + account);
        }

        if(topupResult != null && StringUtils.isEmpty(topupResult.getTxHash())) {
            throw new RemoteAvmCallException("Topup transaction failed. Something is wrong.");
        }

        //Let's try to get receipt
        remoteAvmAdapter.startGetReceipt(topupResult.getTxHash(), "tail", "silent", null, log);

    }

    private void invokeContractForBalanceTopup(String pk, String account) throws RemoteAvmCallException {
        Class localAvmClazz = getLocalAVMClass();
        //Lets do method call encoding

        Method encodeMethodCallMethod = null;
        try {
            encodeMethodCallMethod = localAvmClazz.getMethod("encodeMethodCall", String.class, Object[].class);
        } catch (NoSuchMethodException e) {
            throw new RemoteAvmCallException(e.getMessage(), e);
        }

        String encodedMethodCall = null;
        try {
            encodedMethodCall = (String)encodeMethodCallMethod.invoke(null, "topUp", new Object[0]);
        } catch (Exception e) {
            throw new RemoteAvmCallException(e.getMessage(), e);
        }

        log.info("Encoded method call data: " + encodedMethodCall);

        RemoteAVMNode remoteAVMNode = null;

        String retData = null;

        retData = remoteAvmAdapter.getRemoteAvmNode().sendRawTransaction(faucetContractAddress, pk, encodedMethodCall, BigInteger.ZERO, defaultGas , defaultGasPrice);

        if(retData != null) {
            //Let's try to get receipt
            remoteAvmAdapter.startGetReceipt(retData, "tail", "silent", null, log);
        }

    }

    //This is needed if the account is a new account with balance zero
    private boolean isFacetWebCallRequired(RemoteAvmAdapter remoteAvmAdapter, String address) {
        BigInteger balance = remoteAvmAdapter.getBalance(address);

        log.info("Fetched existing balance for the account : " + balance);

        if(balance == null || BigInteger.ZERO.equals(balance)) {
            log.debug("Address balance is null. Let's try to get some minimum balance for the account through Faucet Web.");
            return true;
        } else {
            return false;
        }
    }

    private TopupResult submitHashCash(String account, HashCash hashCash) throws UnirestException {
        HttpResponse<TopupResult> httpResponse =  Unirest.post(faucetWebUrl + "/register")
                .header("Content-Type", "text/plain")
                .body(hashCash.toString())
                .asObject(TopupResult.class);

        if(httpResponse.getStatus() != 200) {
            return null;
        } else {
            return httpResponse.getBody();
        }

    }

    private Challenge getChallenge() throws UnirestException {
        return Unirest.get(faucetWebUrl + "/challenge")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .asObject(Challenge.class).getBody();
    }

    private Class getLocalAVMClass() {
        try {
            return avmJarClassLoader.loadClass("org.aion4j.avm.helper.local.LocalAvmNode");
        } catch (ClassNotFoundException e) {
            throw new AVMRuntimeException("LocalAvmNode class not found", e);
        }
    }


}
