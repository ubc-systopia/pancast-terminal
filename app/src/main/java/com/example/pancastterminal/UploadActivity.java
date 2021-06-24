package com.example.pancastterminal;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UploadActivity extends Activity {

    private final static String TAG = "UPLOAD";

    // Intent extra codes
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String deviceAddress;
    private EditText otpText;
    private DongleBleService bleService;
    private TextView status;

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "Connecting service");
            bleService = ((DongleBleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.d(TAG, "Connecting device");
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnected");
            bleService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ACTIVITY", "Welcome to the upload activity");

        setContentView(R.layout.activity_upload);

        final Intent intent = getIntent();

        ((TextView) findViewById(R.id.upload_title)).setText(
                "Uploading from " + intent.getStringExtra(EXTRAS_DEVICE_NAME));

        this.deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        this.otpText = findViewById(R.id.otp_field);

        UploadActivity context = this;
        ((Button) findViewById(R.id.otp_submit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.submitOTP();
            }
        });

        status = (TextView) findViewById(R.id.upload_status);
        status.setText("DISCONNECTED");

        Intent gattServiceIntent = new Intent(this, DongleBleService.class);
        bindService(gattServiceIntent, this.serviceConnection, BIND_AUTO_CREATE);
    }

    private void submitOTP()
    {
        int otp;
        String input = otpText.getText().toString();
        otpText.setText("");
        try {
            otp = Integer.parseInt(input);
        } catch (Exception e) {
            Toast.makeText(this, R.string.invalid_otp, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("OTP", "code: " + otp);
        final Intent intent = new Intent(DongleBleService.ACTION_SUBMIT_OTP);
        intent.putExtra(DongleBleService.EXTRA_USER_OTP, otp);
        sendBroadcast(intent);
    }

    public static final String ACTION_SUBMIT_OTP = "com.example.pancastterminal.ACTION_SUBMIT_OTP";

    private IntentFilter intentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        return intentFilter;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(this.receiver, intentFilter());
        if (this.bleService != null) {
            final boolean result = this.bleService.connect(this.deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
            status.setText("CONNECTED");
        } else {
            Log.d(TAG, "Service is null");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleService.disconnect();
        unbindService(this.serviceConnection);
        this.bleService = null;
    }

}
