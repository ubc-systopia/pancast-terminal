package com.example.pancastterminal;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class ScanActivity extends ListActivity {

    private Handler                         handler;
    private BluetoothAdapter                bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback onDeviceScanned;
    private ScanListAdapter                 listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d("SCAN", "Welcome to the scan activity");
        //getActionBar().setTitle(R.string.title_dongles);

        this.handler = new Handler();

        // Initialize the adapter reference

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        this.bluetoothAdapter = bluetoothManager.getAdapter();

        // Bluetooth device checks per the example
        // Checks if Bluetooth is supported on the device.
        if (this.bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Scan Callback
        ScanActivity context = this;
        this.onDeviceScanned = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d("SCAN", "Device found: " + device.getName());
                        //Log.d("SCAN", "Address: " + device.getAddress());
                        if (UUIDParser.serviceIdMatch(scanRecord)) {
                            Log.d("SCAN", "Service ID match!");
                            context.listAdapter.addDevice(device);
                            context.listAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        };
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // prompt for bluetooth enable
        if (!this.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }

        // set up list adapter to store found devices
        this.listAdapter = new ScanListAdapter(this);
        this.setListAdapter(this.listAdapter);

        // do scan
        ScanActivity context = this;
        this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                context.bluetoothAdapter.stopLeScan(context.onDeviceScanned);
                invalidateOptionsMenu();
            }
        },
                // Scan for 3 seconds
                3000 );

        this.bluetoothAdapter.startLeScan(this.onDeviceScanned);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // Bluetooth enable rejected
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.bluetoothAdapter.stopLeScan(this.onDeviceScanned);
        this.listAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = this.listAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this,UploadActivity.class);
        intent.putExtra(UploadActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(UploadActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        this.bluetoothAdapter.stopLeScan(this.onDeviceScanned);
        startActivity(intent);
    }

}
