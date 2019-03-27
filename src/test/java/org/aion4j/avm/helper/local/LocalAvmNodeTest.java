package org.aion4j.avm.helper.local;

import org.aion.avm.api.Address;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion4j.avm.helper.exception.CallFailedException;
import org.aion4j.avm.helper.util.HexUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class LocalAvmNodeTest {

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
        Object decoded = ABIDecoder.decodeOneObject(bytes);
        System.out.println(decoded);

        assertEquals("John", decoded);
    }

    @Test
    public void encodeDeployArgsStringSingleArgsInt() throws CallFailedException {
        String cmdArgs = "-I 8000";

        byte[] bytes = LocalAvmNode.encodeDeployArgsString(cmdArgs);
        Object decoded = ABIDecoder.decodeOneObject(bytes);
        System.out.println(decoded);

        assertEquals(8000, decoded);
    }
}