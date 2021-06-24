package com.example.pancastterminal;

public class Constants {

    public static final String WEB_PROTOCOL = "https";
    public static final String BACKEND_ADDR = "10.0.0.15";
    public static final String BACKEND_PORT = "8081";

    public static final int BT_DATA_UUID128_ALL = 0x07;
    public static final String DONGLE_SERVICE_UUID = "E7F72A03-803B-410A-98D4-4BE5FAD8E217";
    public static final String DONGLE_CHARACT_UUID = "E7F72A03-803B-410A-98D4-4BE5FAD8E218";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static int ENCOUNTER_COUNT_SIZE = 4;
    public static int BEACON_ID_SIZE = 4;
    public static int BEACON_TIMER_SIZE = 4;
    public static int DONGLE_TIMER_SIZE = 4;
    public static int EPH_ID_SIZE = 14;
    public static int LOCATION_ID_SIZE = 8;

}
