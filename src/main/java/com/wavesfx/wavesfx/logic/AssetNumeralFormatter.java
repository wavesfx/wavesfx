package com.wavesfx.wavesfx.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AssetNumeralFormatter {


    public static String toReadable(final Long digit, final int decimals) {
        final var digitBigDecimal = new BigDecimal(digit);
        final var divisor = new BigDecimal(Math.pow(10, decimals));
        return digitBigDecimal.divide(divisor, decimals, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }

    public static long toLong(final String digit, final int decimals) throws NumberFormatException, ArithmeticException{
        if (digit.isEmpty()) return 0L;
        final var formatted = digit.replaceAll(",(?=[0-9]+,)", "").replaceAll(",", ".");
        final var digitBigDecimal = new BigDecimal(formatted).setScale(decimals, RoundingMode.DOWN);
        final var factor = new BigDecimal(Math.pow(10, decimals));
        return digitBigDecimal.multiply(factor).longValueExact();
    }

    public static BigDecimal toBigDecimal(final Long digit, final int decimals) throws NumberFormatException {
        final var digitBigDecimal = new BigDecimal(digit);
        final var divisor = new BigDecimal(Math.pow(10, decimals));
        return digitBigDecimal.divide(divisor, decimals, RoundingMode.DOWN);
    }

    public static String fromBigDecimalToReadable(final BigDecimal digit, final int decimals) throws NumberFormatException {
        return digit.setScale(decimals, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }
}
