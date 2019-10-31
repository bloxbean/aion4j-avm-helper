package org.aion4j.avm.helper.util;

import avm.Address;
import org.aion.avm.tooling.ABIUtil;
import org.aion4j.avm.helper.exception.MethodArgsParseException;
import org.junit.Test;

import java.math.BigInteger;

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

        String argsStr = "-A 0xa0c40d2bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c6ea 0xa0c5122bb3e0248b16f4f1bd5735dc22cd84b580bbc301f8cebc83e25030c3ad -J 100 -I 45 -B 1 -T hello 'My name is John' -I 9 -K 5000000000000000000000";

        Object[] objects = MethodCallArgsUtil.parseMethodArgs(argsStr);

        assertEquals(7, objects.length);
        assertTrue(((Object [])(objects[0]))[0]  instanceof  Address);
        assertTrue(((Object [])(objects[0]))[1]  instanceof  Address);

        assertTrue(((Object [])(objects[4]))[0]  instanceof  String);
        assertTrue(((Object [])(objects[4]))[1]  instanceof  String);

        assertEquals("My name is John", ((Object [])(objects[4]))[1]);
        assertEquals(9, objects[5]);

        assertTrue(objects[6] instanceof BigInteger);
        assertEquals(new BigInteger("5000000000000000000000"), objects[6]);
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
                "-B 1 2 " +
                "-K 900000000000000000000 567900000000000098 8900000000000087665544 " +
                "-I 655 9555 ";

        Object[] args = MethodCallArgsUtil.parseMethodArgs(argStr);
        byte[] arguments = ABIUtil.encodeMethodArguments("testArray", args);

        assertNotNull(args);

        assertEquals(1.2, ((float[])args[5])[0], 0.1);
        assertEquals(3.4, ((float[])args[5])[1], 0.1);

        assertEquals(true, ((boolean[])args[7])[0]);
        assertEquals(false, ((boolean[])args[7])[1]);

        assertEquals(2, ((byte[])args[8])[1]);

        assertEquals(new BigInteger("900000000000000000000"), ((BigInteger[])args[9])[0] );
        assertEquals(new BigInteger("567900000000000098"), ((BigInteger[])args[9])[1] );
        assertEquals(new BigInteger("8900000000000087665544"), ((BigInteger[])args[9])[2] );

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
                "-B[][] '1 1' '0 1' " +
                "-K[][] '500000000000000 78900000000900000000' '877780000000000000000 445555333333333333'";

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

        assertEquals(new BigInteger("500000000000000"), ((BigInteger[][]) objects[12])[0][0]);
        assertEquals(new BigInteger("78900000000900000000"), ((BigInteger[][]) objects[12])[0][1]);
        assertEquals(new BigInteger("877780000000000000000"), ((BigInteger[][]) objects[12])[1][0]);
        assertEquals(new BigInteger("445555333333333333"), ((BigInteger[][]) objects[12])[1][1]);
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

    @Test
    public void testPrint1DArrayBigInteger() {
        BigInteger[] bigIntegers = new BigInteger[]{new BigInteger("788399999993434834783748374"),
                new BigInteger("2392739273927392739223"), new BigInteger("1251725172517251725172512")};

        String printRes = MethodCallArgsUtil.printArray(bigIntegers);

        assertEquals("[788399999993434834783748374, 2392739273927392739223, 1251725172517251725172512]", printRes.trim());

        System.out.println(printRes);
    }

    @Test
    public void testPrint2DArrayBigInteger() {
        BigInteger[][] bigIntegers = new BigInteger[][]{
                {new BigInteger("788399999993434834783748374"),
                new BigInteger("2392739273927392739223"), new BigInteger("1251725172517251725172512")},
                {new BigInteger("424242342342342342342"),
                        new BigInteger("454545454545454545454656"), new BigInteger("00095545343434343111111111111")}
                };

        String printRes = MethodCallArgsUtil.print2DArray(bigIntegers);

        assertEquals("[788399999993434834783748374, 2392739273927392739223, 1251725172517251725172512]\n" +
                "[424242342342342342342, 454545454545454545454656, 95545343434343111111111111]", printRes.trim());

        System.out.println(printRes);
    }

    @Test
    public void testByteArray() throws Exception {
        String arg = "-B 1 2 4 5";
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(arg);
        System.out.println(objs);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[]);
        assertEquals(4, ((byte[])objs[0]).length);
    }

    @Test
    public void testByte2DArray() throws Exception {
        String arg = "-B[][] \"1 2\" \"4 5\"";
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(arg);
        System.out.println(objs);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[][]);
        assertEquals((byte)1, ((byte[][])objs[0])[0][0]);
        assertEquals((byte)2, ((byte[][])objs[0])[0][1]);
        assertEquals((byte)4, ((byte[][])objs[0])[1][0]);
        assertEquals((byte)5, ((byte[][])objs[0])[1][1]);
    }

    @Test
    public void testByte2DArrayOtherFormat() throws Exception {
        String arg = "-B[][] 1,2 4,5";
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(arg);
        System.out.println(objs);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[][]);
        assertEquals((byte)1, ((byte[][])objs[0])[0][0]);
        assertEquals((byte)2, ((byte[][])objs[0])[0][1]);
        assertEquals((byte)4, ((byte[][])objs[0])[1][0]);
        assertEquals((byte)5, ((byte[][])objs[0])[1][1]);
    }


    @Test
    public void testHexValueInByteOption() throws Exception {
        String hexValue = "0xa02d00e708e8a865e67a06f7e7b9eeef748725ee5974d804778426de05b37f9c";

        String args = "-B " + hexValue;
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(args);
        System.out.println(objs);

        byte[] result = (byte[])objs[0];
        String resultHex = "0x" + HexUtil.bytesToHexString(result);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[]);
        assertEquals(32, ((byte[])objs[0]).length);
        assertEquals(hexValue, resultHex);
    }

    @Test
    public void testHexValueInByteOption1DArray() throws Exception {
        String hexValue = "0xa02d00e708e8a865e67a06f7e7b9eeef748725ee5974d804778426de05b37f9c";

        String args = "-B[] " + hexValue;
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(args);
        System.out.println(objs);

        byte[] result = (byte[])objs[0];
        String resultHex = "0x" + HexUtil.bytesToHexString(result);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[]);
        assertEquals(32, ((byte[])objs[0]).length);
        assertEquals(hexValue, resultHex);
    }

    @Test
    public void testHexValueInByteOptionArray() throws Exception {
        String hexValues = "0xa02d00e708e8a865e67a06f7e7b9eeef748725ee5974d804778426de05b37f9c 0xa03d13c46e913fee9316f363513d085915654c0bcda55bd7b4d8d2c7889b288f 0xa092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca";

        String args = "-B[][] " + hexValues;
        Object[] objs = MethodCallArgsUtil.parseMethodArgs(args);
        System.out.println(objs);

        assertTrue(objs.length == 1);
        assertTrue(objs[0] instanceof byte[][]);
        byte[][] bs = (byte[][])objs[0];

        assertEquals(32, bs[0].length);
        assertEquals(32, bs[1].length);
        assertEquals(32, bs[2].length);

        assertEquals("a02d00e708e8a865e67a06f7e7b9eeef748725ee5974d804778426de05b37f9c", HexUtil.bytesToHexString(bs[0]));
        assertEquals("a03d13c46e913fee9316f363513d085915654c0bcda55bd7b4d8d2c7889b288f", HexUtil.bytesToHexString(bs[1]));
        assertEquals("a092de3423a1e77f4c5f8500564e3601759143b7c0e652a7012d35eb67b283ca", HexUtil.bytesToHexString(bs[2]));
    }
}
