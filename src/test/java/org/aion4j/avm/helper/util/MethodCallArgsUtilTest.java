package org.aion4j.avm.helper.util;

import avm.Address;
import org.aion.avm.tooling.ABIUtil;
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
        byte[] arguments = ABIUtil.encodeMethodArguments("testArray", args);

        assertNotNull(args);

    }

    @Test
    public void testEncodeMethodArgsMix() throws  Exception {
        String argStr = "-T first second " +
                "-I 6 9 " +
                "-T test " +
                "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea " +
                "-S 4 5 " +
                "-F 1.2 3.4 " +
                "-D 1.2 2.1  " +
                "-Z true false " +
                "-B 1 2";

        Object[] args = MethodCallArgsUtil.parseMethodArgs(argStr);
        byte[] arguments = ABIUtil.encodeMethodArguments("testArray", args);

        assertNotNull(args);

        assertEquals(1.2, ((float[])args[5])[0], 0.1);
        assertEquals(3.4, ((float[])args[5])[1], 0.1);

        assertEquals(true, ((boolean[])args[7])[0]);
        assertEquals(false, ((boolean[])args[7])[1]);

        assertEquals(2, ((byte[])args[8])[1]);

    }

    @Test
    public void testEncodeMethodArgs2DArray() throws Exception {
        String argStr = "-T[][] \"first second\" \"third fourth\" -I[][] \"4 5\" \"6 7\"";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argStr);

        assertEquals("first", ((String[][]) objects[0])[0][0]);
        assertEquals("second", ((String[][]) objects[0])[0][1]);
        assertEquals("third", ((String[][]) objects[0])[1][0]);
        assertEquals("fourth", ((String[][]) objects[0])[1][1]);

        assertEquals(4, ((int[][]) objects[1])[0][0]);
        assertEquals(5, ((int[][]) objects[1])[0][1]);
        assertEquals(6, ((int[][]) objects[1])[1][0]);
        assertEquals(7, ((int[][]) objects[1])[1][1]);
    }

    @Test
    public void testEncodeMethodArgs2DArray2() throws Exception {
        String argStr = "-T[][] \"first second third fourth\" \"five six 'I am seven' eight\" -I[][] \"4 5 9\" \"6 7\"";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argStr);

        assertEquals("first", ((String[][]) objects[0])[0][0]);
        assertEquals("second", ((String[][]) objects[0])[0][1]);
        assertEquals("third", ((String[][]) objects[0])[0][2]);
        assertEquals("fourth", ((String[][]) objects[0])[0][3]);

        assertEquals("I am seven", ((String[][]) objects[0])[1][2]);

        assertEquals(4, ((int[][]) objects[1])[0][0]);
        assertEquals(5, ((int[][]) objects[1])[0][1]);
        assertEquals(6, ((int[][]) objects[1])[1][0]);
        assertEquals(7, ((int[][]) objects[1])[1][1]);
    }

    @Test
    public void testEncodeMethodArgs2DArray3() throws Exception {
        String argStr = "-T[][] \"first second third fourth\" \"five six 'I am seven' eight\" \"nine ten 'eleven me' twelve\" -I[][] \"4 5 9\" \"6 7\"";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argStr);

        assertEquals("first", ((String[][]) objects[0])[0][0]);
        assertEquals("second", ((String[][]) objects[0])[0][1]);
        assertEquals("third", ((String[][]) objects[0])[0][2]);
        assertEquals("fourth", ((String[][]) objects[0])[0][3]);

        assertEquals("I am seven", ((String[][]) objects[0])[1][2]);

        assertEquals("eleven me", ((String[][]) objects[0])[2][2]);

        assertEquals(4, ((int[][]) objects[1])[0][0]);
        assertEquals(5, ((int[][]) objects[1])[0][1]);
        assertEquals(6, ((int[][]) objects[1])[1][0]);
        assertEquals(7, ((int[][]) objects[1])[1][1]);

    }

    @Test
    public void testEncodeMethodArgs2DArray4() throws Exception {
        String argStr = "-T[][] 'first second' 'third fourth' " +
                "-I 8 9 " +
                "-T myname " +
                "-A[][] '0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c66d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c5cc' " +
                " '0xa0ff0d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0dd6d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c5cc' " +
                "-Z false true " +
                "-Z[][] 'true false' 'true true' " +
                "-J[][] '40 50' '60 70' '80 90' " +
                "-C[][] 'a b c' 'd e f' " +
                "-S[][] '4 5' '9 10' " +
                "-F[][] '1.2 3.2' '4.5 9.8' '6.5 77.0' " +
                "-D[][] '1000.1 40000'  800000 '40000 78000' " +
                "-B[][] '1 1' '0 1' ";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argStr);

        assertEquals("first", ((String[][]) objects[0])[0][0]);
        assertEquals("second", ((String[][]) objects[0])[0][1]);
        assertEquals("third", ((String[][]) objects[0])[1][0]);
        assertEquals("fourth", ((String[][]) objects[0])[1][1]);

        assertEquals("myname", (String) objects[2]);

        assertEquals("a0ff0d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea", ((Address[][]) objects[3])[1][0].toString());

        assertEquals(true, ((boolean[]) objects[4])[1]);

        assertEquals(false, ((boolean[][]) objects[5])[0][1]);
        assertEquals(true, ((boolean[][]) objects[5])[1][0]);

        assertEquals(70, ((long[][]) objects[6])[1][1]);

        assertEquals('e', ((char[][]) objects[7])[1][1]);
        assertEquals('b', ((char[][]) objects[7])[0][1]);

        assertEquals(9, ((short[][]) objects[8])[1][0]);
        assertEquals(10, ((short[][]) objects[8])[1][1]);

        assertEquals(9.8, ((float[][]) objects[9])[1][1], 0.1);
        assertEquals(77.0, ((float[][]) objects[9])[2][1], 0.1);

        assertEquals(800000, ((double[][]) objects[10])[1][0], 0.1);
        assertEquals(40000, ((double[][]) objects[10])[2][0], 0.1);

        assertEquals(1, ((byte[][]) objects[11])[1][1]);
        assertEquals(1, ((byte[][]) objects[11])[0][1]);
    }

    @Test
    public void testEncodeMethodArgs2DArrayWithCommas() throws Exception {
        String argStr = "-T[][] \"first,second,third,fourth\" \"five, six, I am seven, eight\" \"nine, ten, eleven me, twelve\" -I[][] '4,5,9' '6,7'";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argStr);

        assertEquals("first", ((String[][]) objects[0])[0][0]);
        assertEquals("second", ((String[][]) objects[0])[0][1]);
        assertEquals("third", ((String[][]) objects[0])[0][2]);
        assertEquals("fourth", ((String[][]) objects[0])[0][3]);

        assertEquals("I am seven", ((String[][]) objects[0])[1][2]);

        assertEquals("eleven me", ((String[][]) objects[0])[2][2]);

        assertEquals(4, ((int[][]) objects[1])[0][0]);
        assertEquals(5, ((int[][]) objects[1])[0][1]);
        assertEquals(6, ((int[][]) objects[1])[1][0]);
        assertEquals(7, ((int[][]) objects[1])[1][1]);

    }
}
