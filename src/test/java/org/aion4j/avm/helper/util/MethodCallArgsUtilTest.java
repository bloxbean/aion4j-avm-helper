package org.aion4j.avm.helper.util;

import org.aion.avm.api.Address;
import org.aion.avm.userlib.abi.ABIEncoder;
import org.aion4j.avm.helper.exception.MethodArgsParseException;
import org.junit.Test;

import static org.junit.Assert.*;

public class MethodCallArgsUtilTest {

    @Test
    public void translateCommandline() throws MethodArgsParseException {
        String argsStr = "-A 0x1122334455667788112233445566778811223344556677881122334455667788 -J 100 -I 45 -B -1 -T hello -T 'My name is John' -I 9";

        String[] tokens = MethodCallArgsUtil.translateCommandline(argsStr);

        assertEquals(tokens.length, 14);
    }

    @Test
    public void translateCommandlineArray() throws MethodArgsParseException {
        String argsStr = "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c5122bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c3ad -J 100 -I 45 -B -1 -T hello 'My name is John' -I 9";

        String[] tokens = MethodCallArgsUtil.translateCommandline(argsStr);

        assertEquals(14, tokens.length);
    }

    @Test
    public void parseMethodArgs() throws Exception {
        String argsStr = "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea  -J 100 -I 45 -B -1 -T 'My name is John' -I 9";

        //String argsStr = "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c5122bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c3ad -J 100 -I 45 -B -1 -T hello 'My name is John' -I 9";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argsStr);

        assertEquals(6, objects.length);
    }

    @Test
    public void parseMethodArgsArray() throws Exception {

        String argsStr = "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c5122bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c3ad -J 100 -I 45 -B 1 -T hello 'My name is John' -I 9";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argsStr);

        assertEquals(6, objects.length);
        assertTrue(((Object [])(objects[0]))[0]  instanceof  Address);
        assertTrue(((Object [])(objects[0]))[1]  instanceof  Address);

        assertTrue(((Object [])(objects[4]))[0]  instanceof  String);
        assertTrue(((Object [])(objects[4]))[1]  instanceof  String);

        assertEquals("My name is John", ((Object [])(objects[4]))[1]);
        assertEquals(9, objects[5]);
    }

    @Test
    public void testEncodeMethodArgs() throws Exception {
        String argStr = "-T first second -I 8 9 -T myname -A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c66d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c5cc";
        //String argStr = "-A 0x1122334455667788112233445566778811223344556677881122334455667788 -J 100 -I 45 -B -1 -T hello -T 'My name is John' -I 9";

        Object[] args = MethodCallArgsUtil.parseMethodArgs(argStr);
        byte[] arguments = ABIEncoder.encodeMethodArguments("testArray", args);

        assertNotNull(args);

    }

    @Test
    public void testEncodeMethodArgsMix() throws  Exception {
        String argStr = "-T first second -I 6 9 -T test -A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea -S 4 5 -F 1.2 3.4 -D 1.2 2.1  -Z true false -B 1 2";

        Object[] args = MethodCallArgsUtil.parseMethodArgs(argStr);
        byte[] arguments = ABIEncoder.encodeMethodArguments("testArray", args);

        assertNotNull(args);

    }
}
