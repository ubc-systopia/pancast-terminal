package com.example.pancastterminal;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class DongleBleService extends Service {

    private final static String TAG = "Dongle_BLE_Service";

    private final static UUID SERVICE_UUID = UUID.fromString(Constants.DONGLE_SERVICE_UUID);
    private final static UUID CHARACT_UUID = UUID.fromString(Constants.DONGLE_CHARACT_UUID);
    private final static UUID CCONFIG_UUID = UUID.fromString(Constants.CLIENT_CHARACTERISTIC_CONFIG);

    private enum BleConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private enum UploadState {
        LOCKED,
        DATA_0,
        DATA_1,
        DATA_2,
        DATA_3,
        DATA_4
    }

    private static final byte DATA_TYPE_OTP =            0x01;
    private static final byte DATA_TYPE_NUM_RECS =       0x02;
    private static final byte DATA_TYPE_ACK_NUM_RECS =   0x03;
    private static final byte DATA_TYPE_DATA_0  =        0x04;
    private static final byte DATA_TYPE_ACK_DATA_0 =     0x05;
    private static final byte DATA_TYPE_DATA_1 =         0x06;
    private static final byte DATA_TYPE_ACK_DATA_1 =     0x07;
    private static final byte DATA_TYPE_DATA_2 =         0x08;
    private static final byte DATA_TYPE_ACK_DATA_2 =     0x09;
    private static final byte DATA_TYPE_DATA_3 =         0x0a;
    private static final byte DATA_TYPE_ACK_DATA_3 =     0x0b;
    private static final byte DATA_TYPE_DATA_4 =         0x0c;
    private static final byte DATA_TYPE_ACK_DATA_4 =     0x0d;

    private Encounter           curEncounter;
    private ArrayList<Encounter> encounters = new ArrayList<>();

    private BleConnectionState  connectionState;

    private UploadState         uploadState = UploadState.LOCKED;

    private BluetoothManager    bluetoothManager;
    private BluetoothAdapter    bluetoothAdapter;
    private String              bluetoothDeviceAddress; // cached address for connection re-use
    private BluetoothGatt       bluetoothGatt;
    private BluetoothGattServer server;

    BluetoothGattCharacteristic characteristic; // writable characteristic used to send data

    private int                 numRecs;
    private int                 numRecsReceived;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = BleConnectionState.CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = BleConnectionState.DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered");
                List<BluetoothGattService> services = getSupportedGattServices();
                if (services == null) {
                    return;
                }
                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(DongleBleService.SERVICE_UUID)) {
                        Log.d(TAG, "Correct service found");
                        BluetoothGattCharacteristic ch
                                = service.getCharacteristic(DongleBleService.CHARACT_UUID);
                        if (ch == null) {
                            return;
                        }
                        Log.d(TAG, "Characteristic found");
                        bluetoothGatt.setCharacteristicNotification(ch, true);
                        BluetoothGattDescriptor descriptor = ch.getDescriptor(CCONFIG_UUID);
                        bluetoothGatt.writeCharacteristic(ch);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "Subscribed");
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Characteristic changed");
            onNotify(characteristic);
        }
    };

    public class LocalBinder extends Binder {
        DongleBleService getService() {
            return DongleBleService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private BluetoothDevice getDevice() {
        final BluetoothDevice device = this.bluetoothAdapter.getRemoteDevice(this.bluetoothDeviceAddress);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
        }
        return device;
    }

    // Intent Handling

    public static final String ACTION_SUBMIT_OTP = "com.example.pancastterminal.ACTION_SUBMIT_OTP";
    public static final String EXTRA_USER_OTP = "com.example.pancastterminal.EXTRA_USER_OTP";

    private IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SUBMIT_OTP);
        return intentFilter;
    }

    public void gattNotify() {
        server.notifyCharacteristicChanged(getDevice(), characteristic, false);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_SUBMIT_OTP:
                    Log.d(TAG, "Submit OTP");
                    int otp = intent.getIntExtra(EXTRA_USER_OTP, 0);
                    characteristic.setValue(Util.concat(DATA_TYPE_OTP,
                                Util.encodeLittleEndian(IntegerContainer.make(otp), 8)));
                    gattNotify();
                    break;
            }
        }
    };

    // BLE Server
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {};

    // Methods pulled from example Service:

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        registerReceiver(this.receiver, intentFilter());

        // Get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (this.bluetoothManager == null) {
            this.bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (this.bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
        if (this.bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        server = this.bluetoothManager.openGattServer(this, this.gattServerCallback);
        if (server == null) {
            Log.w(TAG, "Unable to create GATT server");
            return false;
        }

        // Declare service
        BluetoothGattService service = new BluetoothGattService(DongleBleService.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic ch
                = new BluetoothGattCharacteristic(DongleBleService.CHARACT_UUID,
BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE,
BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PROPERTY_WRITE);

        ch.addDescriptor(new BluetoothGattDescriptor(DongleBleService.CCONFIG_UUID,
    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));

        service.addCharacteristic(ch);

        server.addService(service);

        characteristic = ch;

        return true;
    }

    private void setDataType(byte type)
    {
        byte[] flags = { type };
        characteristic.setValue(flags);
    }

    private void onNotify(BluetoothGattCharacteristic ch)
    {
        byte[] data = ch.getValue();

        switch (uploadState) {
            case LOCKED:
                if (data[0] != DATA_TYPE_NUM_RECS) {
                    break;
                }
                numRecs = Util.decodeLittleEndian(data,
                        1, Constants.ENCOUNTER_COUNT_SIZE).intValue();
                Log.d(TAG, "Number of records: " + numRecs);
                if (numRecs > 0) {
                    uploadState = UploadState.DATA_0;
                    numRecsReceived = 0;
                    Log.d(TAG, "Fetching...");
                }
                setDataType(DATA_TYPE_ACK_NUM_RECS);
                gattNotify();
                break;
            case DATA_0:
                if (data[0] != DATA_TYPE_DATA_0) {
                    break;
                }
                Log.d(TAG, "Receiving data 0");
                curEncounter = new Encounter();
                curEncounter.beaconId = Util.decodeLittleEndian(data,
                        1, Constants.BEACON_ID_SIZE);
                uploadState = UploadState.DATA_1;
                setDataType(DATA_TYPE_ACK_DATA_0);
                gattNotify();
                break;
            case DATA_1:
                if (data[0] != DATA_TYPE_DATA_1) {
                    break;
                }
                Log.d(TAG, "Receiving data 1");
                curEncounter.beaconTime = Util.decodeLittleEndian(data,
                        1, Constants.BEACON_TIMER_SIZE);
                uploadState = UploadState.DATA_2;
                setDataType(DATA_TYPE_ACK_DATA_1);
                gattNotify();
                break;
            case DATA_2:
                if (data[0] != DATA_TYPE_DATA_2) {
                    break;
                }
                Log.d(TAG, "Receiving data 2");
                curEncounter.dongleTime = Util.decodeLittleEndian(data,
                        1, Constants.DONGLE_TIMER_SIZE);
                uploadState = UploadState.DATA_3;
                setDataType(DATA_TYPE_ACK_DATA_2);
                gattNotify();
                break;
            case DATA_3:
                if (data[0] != DATA_TYPE_DATA_3) {
                    break;
                }
                Log.d(TAG, "Receiving data 3");
                curEncounter.ephId = Base64.getEncoder().encodeToString(Util.copy(data,
                        1, Constants.EPH_ID_SIZE));
                uploadState = UploadState.DATA_4;
                setDataType(DATA_TYPE_ACK_DATA_3);
                gattNotify();
                break;
            case DATA_4:
                if (data[0] != DATA_TYPE_DATA_4) {
                    break;
                }
                Log.d(TAG, "Receiving data 4");
                curEncounter.locationId = Util.decodeLittleEndian(data,
                        1, Constants.LOCATION_ID_SIZE);
                numRecsReceived++;
                encounters.add(curEncounter);
                if (numRecsReceived == numRecs) {
                    Log.i(TAG, "All records received");
                    // upload the encounters
                    String result = MainActivity.Companion.makeRequest(encounters);
                    Log.d(TAG, "Request result: " + result);
                }
                uploadState = UploadState.DATA_0;
                setDataType(DATA_TYPE_ACK_DATA_4);
                gattNotify();
                break;
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (this.bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (this.bluetoothDeviceAddress != null && address.equals(this.bluetoothDeviceAddress)
                && this.bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (this.bluetoothGatt.connect()) {
                this.connectionState = BleConnectionState.CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        this.bluetoothDeviceAddress = address;

        final BluetoothDevice device = this.getDevice();
        if (device == null) {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        this.bluetoothGatt = device.connectGatt(this, false, this.gattCallback);
        Log.d(TAG, "Trying to create a new connection.");

        this.connectionState = BleConnectionState.CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (this.bluetoothAdapter == null || this.bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        this.bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (this.bluetoothGatt == null) {
            return;
        }
        this.bluetoothGatt.close();
        this.bluetoothGatt = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (this.bluetoothGatt == null) return null;
        return this.bluetoothGatt.getServices();
    }

}
