package com.example.pancastterminal;

import android.util.Log;

import java.math.BigInteger;
import java.util.List;

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

    public static byte[] encodeLittleEndian(BigInteger i, int size)
    {
        //Log.d("UTIL", "Encoding " + i.toString());
        byte[] result = new byte[size];
        for (int j = 0; j < size; j++) {
            result[j] = (byte) (i.and(new BigInteger(new Integer(0xff).toString())).intValue());
            i = i.shiftRight(8);
        }
        return result;
    }

    public static BigInteger decodeLittleEndian(byte[] src, int off, int size)
    {
        BigInteger i = new IntegerContainer(0);
        for (int j = off; j < off + size; j++) {
            i = i.add(IntegerContainer.make((src[j] << ((j-off) * 8)) & 0xff));
        }
        if (!i.abs().equals(i)) {
            // debugging for negative value
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

    public static String makeUploadRequest(List<Encounter> encounters)
    {
        StringBuilder bld = new StringBuilder();
        bld.append("{\"Entries\": [");
        for (int i = 0; i < encounters.size(); i++) {
            Encounter en = encounters.get(i);
            bld.append(String.format("{"        +
                    "\"EphemeralID\": \"%s\","  +
                    "\"DongleClock\": %s,"      +
                    "\"BeaconClock\": %s,"      +
                    "\"BeaconId\":    %s,"      +
                    "\"LocationID\":  %s"       +
                    "}", en.ephId, en.dongleTime, en.beaconTime, en.beaconId, en.locationId));
            if (i < encounters.size() - 1) {
                bld.append(',');
            }
        }
        bld.append("], \"Type\": 0 }");
        return bld.toString();
    }

}
