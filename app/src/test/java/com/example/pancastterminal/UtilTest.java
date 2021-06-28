package com.example.pancastterminal;

import org.junit.Test;

import org.junit.Assert;

import java.math.BigInteger;

public class UtilTest {

    @Test
    public void testEncode()
    {
        byte[] data = Util.encodeLittleEndian(IntegerContainer.make(12345678), 8);
        byte[] expected = { 0x4e, 0x61, (byte) 0xbc, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Assert.assertEquals(expected[0], data[0]);
        Assert.assertEquals(expected[1], data[1]);
        Assert.assertEquals(expected[2], data[2]);
        Assert.assertEquals(expected[3], data[3]);
        Assert.assertEquals(expected[4], data[4]);
        Assert.assertEquals(expected[5], data[5]);
        Assert.assertEquals(expected[6], data[6]);
        Assert.assertEquals(expected[7], data[7]);
    }

    @Test
    public void testDecode()
    {
        byte[]data = { 0x64, 0x00, 0x00, 0x00, 0x00, 0x01 };
        int smolResult = Util.decodeLittleEndian(data, 0, 4).intValue();
        Assert.assertEquals(100, smolResult);
    }

}
