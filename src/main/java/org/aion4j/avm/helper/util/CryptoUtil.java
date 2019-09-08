package org.aion4j.avm.helper.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CryptoUtil {
    public static final BigInteger ONE_AION = new BigInteger("1000000000000000000"); //1 Aion

    public static BigDecimal ampToAion(BigInteger value) {
        if(value != null) {
            return new BigDecimal(value).divide(new BigDecimal(ONE_AION));
        } else {
            return BigDecimal.ZERO;
        }
    }

    public static BigInteger aionTonAmp(double aion) {
        BigDecimal bigDecimalAmt = new BigDecimal(aion);
        BigDecimal nAmp = new BigDecimal(ONE_AION).multiply(bigDecimalAmt);

        return nAmp.toBigInteger();
    }
}
