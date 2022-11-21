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

import com.example.appsoa2.interfaces.MainActivityContract;
import com.example.appsoa2.presenters.MainPresenter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainModel implements MainActivityContract.ModelMVP {

    private BluetoothDevice primaryDevice = null;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int MULTIPLE_PERMISSIONS = 10; // code you want.
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
    // private static String MAC_ADDRESS_DEVICE = "14:08:13:06:51:24"; // TEST  - MIBAND DEVICE

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
            // Tengo el device conectado?
            if (mBluetoothAdapter == null) {
                // Tengo el bluetooth prendido?
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            enableComponent();
        }
        // Sino, aviso. Podria intentar conectarlo pero con avisar y entrar conetado ta bien.
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

    @Override
    public void processDataGetResult(OnSendToPresenter presenter) {

    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 6
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
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                response = "Bluetooth ya encendido!!";
                if (!primaryDeviceIsAlreadyConnected()) {
                    searchBluetoothDevices();
                }
            } else {
                response = "Bluetooth apagado... Necesitas encenderlo!";
                this.currentPresenter.askBTPermission();
            }
        }
        this.currentPresenter.showOnToast(response);
        this.currentPresenter.showOnLabel(response);
    }

    private void initializeBroadcastReceiver(Context context) {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); // Cambia el estado del Bluethoot (Activando /Desactivado)
        filter.addAction(BluetoothDevice.ACTION_FOUND); // Se encuentra un dispositivo bluethoot al realizar una busqueda
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); // Cuando se comienza una busqueda de bluethoot
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // Cuando la busqueda de bluethoot finaliza
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); // Cuando se empareja o desempareja el bluethoot
        // Se define (registra) el handler que captura los broadcast anterirmente mencionados.
        context.registerReceiver(mReceiver, filter);
    }

    private void searchBluetoothDevices() {
        mBluetoothAdapter.startDiscovery();
    }

    private void handleBluetoothEvent(Intent intent, String action) {
        String response = null;
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            // Cambio el estado del bluetooth
            //Obtengo el parametro, aplicando un Bundle, que me indica el estado del Bluethoot
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            //Si esta activado
            if (state == BluetoothAdapter.STATE_ON) {
                response = "Tu bluetooth ya esta activo";
                this.currentPresenter.showOnLabel(response);
                this.currentPresenter.showOnToast(response);
                searchBluetoothDevices();
                this.currentPresenter.enableButtons();
            } else if(state == BluetoothAdapter.STATE_OFF) {
                response = "Habilita el bluetooth para continuar...";
                this.currentPresenter.showOnToast(response);
                this.currentPresenter.disableButtons();
                this.currentPresenter.askBTPermission(); // Revisar si vale la pena
            }
        }
        else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            // Arranco a buscar dispositivos
            this.currentPresenter.showLoadingDialog();
            this.currentPresenter.disableButtons();
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            //Termino de buscar dispositivos
            this.currentPresenter.closeLoadingDialog();

            // connectPrimaryDevice();
            if (primaryDevice == null) {
                response = "No se encontro cortina disponible";
                this.currentPresenter.showOnLabel(response);
                this.currentPresenter.showOnToast(response);
                this.currentPresenter.disableButtons();
            }

        }
        else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            //Cada vez que encuentro un dispositivo bluetooth cercano

            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            this.currentPresenter.showOnToast("Dispositivo cercano: " + device.getName());
            if (checkPrimaryDevice(device)) {
                primaryDevice = device;
                finishBluetoothSearch();
                this.currentPresenter.enableButtons();
            }
        } // No se si vale la pena todo lo de abajo.
        /*else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            // Obtengo los parametro, aplicando un Bundle, que me indica el estado del Bluethoot
            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

            if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                //Si se detecto que se puedo emparejar el bluethoot
                this.currentPresenter.showOnToast("Proceso de conexion bt finalizado!");
                BluetoothDevice dispositivo = (BluetoothDevice) primaryDevice;
                //se inicia el Activity de comunicacion con el bluethoot, para transferir los datos.

            } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                this.currentPresenter.showOnToast("Dispositivo desemparejado");
                primaryDevice = null;
            }
        }*/
    }

    private boolean checkPrimaryDevice(BluetoothDevice currentDevice) {
        if (currentDevice.getAddress().equals(MAC_ADDRESS_DEVICE)) {
            if (currentDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                this.currentPresenter.showOnToast("Dispositivo cortina ya conectada!");
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
            this.currentPresenter.showOnToast("Cortina cercana... Emparejando...");
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
        this.currentPresenter.consoleLog("Dispositivos ", String.valueOf(pairedDevices));
        if (pairedDevices == null || pairedDevices.size() == 0) {
            this.currentPresenter.showOnToast("No se encontraron dispositivos emparejados");
            this.primaryDevice = null;
        } else {
            for (BluetoothDevice currentDevice : pairedDevices) {
                if (currentDevice.getAddress().equals(MAC_ADDRESS_DEVICE)) {
                    if (currentDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        this.currentPresenter.showOnLabel("Cortina " + currentDevice.getName() + " ya se encuentra conectada...");
                        this.currentPresenter.showOnToast("Paireado de cortina finalizado conectada!!!");
                    } else { // Caso extranio, analizar... Si estoy buscando entre dispositivos conectados y no esta enlazada, deberia ser un error.
                        // Intento reconectarla.
                        pairDevice(currentDevice);
                    }
                    this.primaryDevice = currentDevice;
                    return true;
                }
            }
        }
        return false;
    }
}
