package org.aion4j.avm.helper.local;

import org.aion.avm.embed.StandardCapabilities;
import org.aion.types.AionAddress;
import org.aion.types.InternalTransaction;

public class TestStandardCapabilities extends StandardCapabilities {

    @Override
    public InternalTransaction decodeSerializedTransaction(byte[] transactionPayload, AionAddress executor, long energyPrice, long energyLimit) {
        return InvokableTxUtil.decode(transactionPayload, executor, energyPrice, energyLimit);
    }
}
