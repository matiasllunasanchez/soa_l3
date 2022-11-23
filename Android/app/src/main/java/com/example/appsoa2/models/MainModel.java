package com.example.appsoa2.models;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.appsoa2.interfaces.MainActivityContract;
import com.example.appsoa2.presenters.MainPresenter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainModel implements MainActivityContract.ModelMVP {
    private static final String TAG = "MainModel";
    private BluetoothDevice primaryDevice = null;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int MULTIPLE_PERMISSIONS = 10;
    private MainActivityContract.ModelMVP.OnSendToPresenter currentPresenter = null;
    private Context currentContext = null;

    String[] permissions = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private static String MAC_ADDRESS_DEVICE = "00:21:06:BE:58:58"; // REAL DEVICE - CORTINA HC-05

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            handleBluetoothEvent(intent, action);
        }
    };


    @Override
    public void getReadyBluetooth(Context context, MainActivityContract.ModelMVP.OnSendToPresenter presenter) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        currentContext = context;
        currentPresenter = presenter;
        if (checkPermissions()) {
            enableComponent();
            initializeBroadcastReceiver(context);
        }
    }

    @Override
    public void stopBluetoothDiscovery() {
        finishBluetoothSearch();
    }

    @Override
    public void onDestroyProcess() {
        currentContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResumeProcess() {
        if (primaryDevice == null) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            enableComponent();
        }
    }

    @Override
    public void onPauseProcess() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    @Override
    public void permissionsGrantedProcess() {
        enableComponent();
        initializeBroadcastReceiver(currentContext);
    }

    @Override
    public String getConnectedMacAddress() {
        return this.primaryDevice.getAddress();
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(currentContext, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            currentPresenter.requestPermissions(listPermissionsNeeded);
            return false;
        }
        return true;
    }

    private void enableComponent() {
        String response = null;

        if (mBluetoothAdapter == null) {
            response = "Bluetooth no es soportado por el dispositivo movil";
            this.currentPresenter.showOnLabel(response);
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                response = "Bluetooth ya encendido!!";
                consoleLog(response,"");
                if (!primaryDeviceIsAlreadyConnected()) {
                    searchBluetoothDevices();
                }
            } else {
                response = "Bluetooth apagado... Necesitas encenderlo!";
                this.currentPresenter.showOnToast(response);
                this.currentPresenter.askBTPermission();
            }
        }

    }

    private void initializeBroadcastReceiver(Context context) {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
    }

    private void searchBluetoothDevices() {
        mBluetoothAdapter.startDiscovery();
    }

    private void handleBluetoothEvent(Intent intent, String action) {
        String response = null;
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            if (state == BluetoothAdapter.STATE_ON) {
                response = "Tu bluetooth activo";
                this.currentPresenter.showOnToast(response);
                searchBluetoothDevices();
                this.currentPresenter.enableButtons();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                this.currentPresenter.showOnLabel("Habilita el bluetooth para continuar...");
                this.currentPresenter.disableButtons();
                this.currentPresenter.askBTPermission();
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            this.currentPresenter.showLoadingDialog();
            this.currentPresenter.disableButtons();
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            this.currentPresenter.closeLoadingDialog();

            if (primaryDevice == null) {
                response = "No se encontro cortina disponible";
                this.currentPresenter.showOnLabel(response);
                this.currentPresenter.disableButtons();
            }

        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
           consoleLog("Dispositivo cercano: ", device.getName());
            if (checkPrimaryDevice(device)) {
                primaryDevice = device;
                finishBluetoothSearch();
                this.currentPresenter.enableButtons();
            }
        }
    }

    private boolean checkPrimaryDevice(BluetoothDevice currentDevice) {
        if (currentDevice.getAddress().equals(MAC_ADDRESS_DEVICE)) {
            if (currentDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                this.currentPresenter.showOnLabel("Cortina " + currentDevice.getName() + " ya conectada!");
            } else {
                pairDevice(currentDevice);
            }
            return true;
        }
        return false;
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            this.currentPresenter.showOnLabel("Cortina cercana... Emparejando...");
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            this.currentPresenter.showOnLabel("Bindeo existoso para dispositivo: " + device.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishBluetoothSearch() {
        this.currentPresenter.closeLoadingDialog();
        mBluetoothAdapter.cancelDiscovery();
    }

    private boolean primaryDeviceIsAlreadyConnected() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.size() == 0) {
            this.currentPresenter.showOnToast("No se encontraron dispositivos emparejados");
            this.primaryDevice = null;
        } else {
            for (BluetoothDevice currentDevice : pairedDevices) {
                if (currentDevice.getAddress().equals(MAC_ADDRESS_DEVICE)) {
                    if (currentDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        this.currentPresenter.showOnToast("Cortina " + currentDevice.getName() + "conectada");
                        this.currentPresenter.showOnLabel("Cortina " + currentDevice.getName() + "conectada");
                    } else {
                        pairDevice(currentDevice);
                        this.currentPresenter.showOnLabel("Paireando Cortina nuevamente");
                    }
                    this.primaryDevice = currentDevice;
                    return true;
                }
            }
        }
        return false;
    }

    private void consoleLog(String label, String data) {
        Log.i(TAG, label + data);
    }

}
