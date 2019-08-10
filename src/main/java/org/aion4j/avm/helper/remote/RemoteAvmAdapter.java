package org.aion4j.avm.helper.remote;

import org.aion.base.util.ByteUtil;
import org.aion4j.avm.helper.api.logs.DummyLog;
import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.api.logs.Slf4jLog;
import org.aion4j.avm.helper.exception.RemoteAvmCallException;
import org.aion4j.avm.helper.util.ResultCache;
import org.aion4j.avm.helper.util.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class RemoteAvmAdapter {
    private final static Logger logger = LoggerFactory.getLogger(RemoteAvmAdapter.class);


    private RemoteAVMNode remoteAvmNode;
    private String nodeUrl;

    public RemoteAvmAdapter(String nodeUrl, Log log) {
        this.remoteAvmNode = new RemoteAVMNode(nodeUrl, log);
        this.nodeUrl = nodeUrl;
    }

    public RemoteAVMNode getRemoteAvmNode() {
        return remoteAvmNode;
    }

    public BigInteger getBalance(String address) {
        if (StringUtils.isEmpty(address)) {
            return null;
        }

        String balanceInHex = remoteAvmNode.getBalance(address);

        if (!StringUtils.isEmpty(balanceInHex)) {
            if (balanceInHex.startsWith("0x"))
                balanceInHex = balanceInHex.substring(2);

            BigInteger balance = new BigInteger(balanceInHex, 16);
            return balance;

        } else {
            return null;
        }
    }

    public BigInteger getNonce(String address) {
        if (StringUtils.isEmpty(address)) {
            return null;
        }

        String nonceInHex = remoteAvmNode.getTransactionCount(address);

        if(!StringUtils.isEmpty(nonceInHex)) {
            BigInteger nonce = ByteUtil.bytesToBigInteger(ByteUtil.hexStringToBytes(nonceInHex));
            return nonce;

        } else
            return null;
    }

    public void startGetReceipt(String txHash, String tail, String silent, ResultCache cache, Log log) throws RemoteAvmCallException {
        boolean enableTail = false;
        if(tail != null && !tail.isEmpty())
            enableTail = true;

        int counter = 0;
        int maxCountrer = 1;
        boolean gotReceipt = false;

        if(enableTail) maxCountrer = 15;
        while(counter < maxCountrer) {
            try {

                Log _log = null;
                if(enableTail && silent != null && !silent.isEmpty()) _log = new DummyLog();
                else {
                    _log = log;

                    if(_log == null) {
                        _log = new Slf4jLog(logger);
                    }
                }

                RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl , _log);

                JSONObject response = remoteAVMNode.getReceipt(txHash);
                JSONObject resultObj = response.optJSONObject("result");

                counter++;

                if (resultObj == null) {
                    if(enableTail) {
                        log.info("Waiting for transaction to mine ...Trying (" + counter + " of " + maxCountrer + " times)");
                        Thread.currentThread().sleep(9000);
                        continue;
                    }
                } else {
                    String contractAddress = resultObj.optString("contractAddress");
                    if (contractAddress != null && !contractAddress.isEmpty()) {
                        //Update contract address in cache.
                        //Update deploy status properties
                        if(cache != null)
                            cache.updateDeployAddress(contractAddress);
                    } else {
                    }

                    //Get status
                    String status = resultObj.optString("status");
                    if(("0x0").equals(status)) {
                        throw new RemoteAvmCallException("Transaction could not be processed.");
                    }
                }

                log.info("Txn Receipt: \n");
                if (resultObj != null) {
                    log.info(resultObj.toString(2));
                } else
                    log.info(response.toString());

                gotReceipt = true;
                break;
            } catch (Exception e) {
                log.debug("Get receipt failed", e);
                throw new RemoteAvmCallException(e.getMessage(), e);
            }
        }

        if(counter == maxCountrer && !gotReceipt) {
            log.info("Waited too long for the receipt, something is wrong.");
        }
    }
}
