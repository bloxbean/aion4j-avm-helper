package org.aion4j.avm.helper;

import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.api.logs.Slf4jLog;
import org.aion4j.avm.helper.faucet.NetworkHelper;
import org.aion4j.avm.helper.faucet.model.Network;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

public class NetworkHelperTest {
    private final Log log = new Slf4jLog(LoggerFactory.getLogger(NetworkHelperTest.class));

    @Test
    public void getNetworksTest() {
        NetworkHelper networksHelper = new NetworkHelper(log);

        List<Network> networks = networksHelper.getNetworks();

        assertTrue(networks.size() > 1);
        assertEquals("mastery", networks.get(0).getId());
        assertEquals("0x1878e70918712e2f4cd9c4f1cfba6ff776b407fcf8ae64b686ab2cc673c18cb5", networks.get(0).getGenesisHash());
        assertEquals("Mastery", networks.get(0).getNetwork());
        assertEquals("0xa055dc67cd05d013a0b7c064708a0eb86e31c5edbaf00bca645665217779d9f2", networks.get(0).getFaucetContract());
    }

    @Test
    public void getNetworkFromWeb3RpcUrlTest() {

    }
}
