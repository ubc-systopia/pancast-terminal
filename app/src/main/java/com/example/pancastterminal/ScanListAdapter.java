package com.example.pancastterminal;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

// Adapted from BluetoothLeGatt example
// List adapter for managing and displaying scanned devices
public class ScanListAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> devices;
    private LayoutInflater inflater;

    public ScanListAdapter(ScanActivity context) {
        super();
        this.devices = new ArrayList<>();
        this.inflater = context.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device) {
        if(!this.devices.contains(device)) {
            this.devices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return this.devices.get(position);
    }

    public void clear() {
        this.devices.clear();
    }

    @Override
    public int getCount() {
        return this.devices.size();
    }

    @Override
    public Object getItem(int i) {
        return this.devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = this.inflater.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = this.devices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }

}
