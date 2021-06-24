package com.example.pancastterminal;

import android.util.Log;

public class Util {

    public static byte[] concat(byte[] a, byte[] b)
    {
        byte[] all = new byte[a.length + b.length];
        int i = 0;
        for (; i < a.length; i++) {
            all[i] = a[i];
        }
        for (int j = i; j < b.length + i; j++) {
            all[j] = b[j-1];
        }
        return all;
    }

    public static byte[] concat(byte b, byte[] c)
    {
        byte[] a = { b };
        return concat(a,c);
    }

    public static byte[] encodeLittleEndian(int i, int size)
    {
        byte[] result = new byte[size];
        for (int j = 0; j < size; j++) {
            result[j] = (byte) (i & 0xff);
            i >>= 8;
        }
        return result;
    }

    public static int decodeLittleEndian(byte[] src, int off, int size)
    {
        int i = 0;
        for (int j = off; j < off + size; j++) {
            i += (src[j] << ((j-off) * 8)) & 0xff;
        }
        if (i < 0) {
            Log.d("UTIL", "value:" + i);
            Log.d("UTIL", "size:" + size);
            for (int j = off; j < off + 4; j++) {
                Log.d("UTIL", "byte: " + String.format("%02x", src[j]));
            }
        }
        return i;
    }

    public static byte[] copy(byte[] src, int off, int size)
    {
        byte[] dst = new byte[size];
        for (int i = off; i < off + size; i++) {
            dst[(i-off)] = src[i];
        }
        return dst;
    }

}
