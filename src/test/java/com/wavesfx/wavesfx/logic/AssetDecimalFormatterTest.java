package com.wavesfx.wavesfx.logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssetDecimalFormatterTest {

    @Test
    public void toReadable() {
        String expected = "1";
        String actual = AssetNumeralFormatter.toReadable(100000000L, 8);
        assertEquals(expected, actual);
    }

    @Test
    public void toLong() {
        Long expected = 430102942L;
        Long actual = AssetNumeralFormatter.toLong("4.30102942", 8);
        assertEquals(expected, actual);
    }
}