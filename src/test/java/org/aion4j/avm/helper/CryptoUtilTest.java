package org.aion4j.avm.helper;

import org.aion4j.avm.helper.util.CryptoUtil;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CryptoUtilTest {

    @Test
    public void testAionTonAmp() {
        BigInteger bi = CryptoUtil.aionTonAmp(5);

        Assert.assertEquals(new BigInteger("5000000000000000000"), bi);
    }

    @Test
    public void testAionTonAmp2() {
        BigInteger bi = CryptoUtil.aionTonAmp(100000);

        Assert.assertEquals(new BigInteger("100000000000000000000000"), bi);
    }

    @Test
    public void testAionTonAmp1() {
        BigInteger bi = CryptoUtil.aionTonAmp(.0005);

        Assert.assertEquals(new BigInteger("500000000000000"), bi);
    }

    @Test
    public void testAmpToAion() {
        BigDecimal aionAmt = CryptoUtil.ampToAion(new BigInteger("1000000000000000000000000000000"));

        System.out.println(aionAmt);
        Assert.assertEquals(new BigDecimal("1000000000000"), aionAmt);
    }

    @Test
    public void testAmpToAion2() {
        BigDecimal aionAmt = CryptoUtil.ampToAion(new BigInteger("500000000000000"));

        System.out.println(aionAmt);
        Assert.assertEquals(new BigDecimal(".0005"), aionAmt);
    }

    @Test
    public void testAmpToAion3() {
        BigDecimal aionAmt = CryptoUtil.ampToAion(new BigInteger("990000000000000000000000000000000"));

        System.out.println(aionAmt);
        Assert.assertEquals(new BigDecimal("990000000000000"), aionAmt);
    }

    @Test
    public void testAmpToAion4() {
        BigDecimal aionAmt = CryptoUtil.ampToAion(new BigInteger("50000000000"));

        System.out.println(aionAmt);
        Assert.assertEquals(new BigDecimal(".00000005"), aionAmt);
    }
}
