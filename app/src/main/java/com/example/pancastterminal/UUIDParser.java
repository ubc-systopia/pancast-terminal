package com.example.pancastterminal;

import android.util.Log;

public class UUIDParser {

    static public boolean serviceIdMatch(byte[] ad)
    {
        if (ad.length < 1) {
            Log.d("PARSE", "Malformed - No Length byte");
            return false;
        }

        int i = 0;

        int length = ad[i];
        //Log.d("PARSE", "Length (including type): " + length);

        if (ad.length - 1 < length) {
            Log.d("PARSE", "Malformed - Too short container");
            return false;
        }

        i++;

        int type = ad[i];

        if (type != Constants.BT_DATA_UUID128_ALL) {
            //Log.d("PARSE", "Wrong type");
            return false;
        }

        if (length - 1 != 16) {
            //Log.d("PARSE", "Malformed - Wrong length");
        }

        i += 16;
        int d  = i - 16;

        StringBuilder uuid = new StringBuilder();

        for (; i-d > 12; i--) {
            uuid.append(String.format("%02X", ad[i]));
        }

        uuid.append('-');

        for (; i-d > 10; i--) {
            uuid.append(String.format("%02X", ad[i]));
        }

        uuid.append('-');

        for (; i-d > 8; i--) {
            uuid.append(String.format("%02X", ad[i]));
        }

        uuid.append('-');

        for (; i-d > 6; i--) {
            uuid.append(String.format("%02X", ad[i]));
        }

        uuid.append('-');

        for (; i-d > 0; i--) {
            uuid.append(String.format("%02X", ad[i]));
        }

        String finalUuid = uuid.toString();
        //Log.d("PARSE", "UUID: " + finalUuid);

        return finalUuid.equals(Constants.DONGLE_SERVICE_UUID);
    }

}
