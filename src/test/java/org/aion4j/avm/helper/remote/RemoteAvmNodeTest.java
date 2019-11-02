package org.aion4j.avm.helper.remote;

import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.api.logs.Slf4jLog;
import org.aion4j.avm.helper.util.StringUtils;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

public class RemoteAvmNodeTest {

    private final static Log log = new Slf4jLog(LoggerFactory.getLogger(RemoteAVMNode.class));

    private static String getWeb3RpcUlr() {
        //return "http://192.168.0.96:8545";
        return System.getProperty("web3rpc.url");
    }

    @Test
    public void getGenesisBlockHash() {
        if(StringUtils.isEmpty(getWeb3RpcUlr())) {
            return;
        }

        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(getWeb3RpcUlr(), log);
        String genesisHssh = remoteAVMNode.getGenesisBlockHash();

        assertThat(genesisHssh, startsWith("0x"));
    }
}
