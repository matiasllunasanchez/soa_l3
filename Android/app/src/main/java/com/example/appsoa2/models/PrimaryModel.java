package com.example.appsoa2.models;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.example.appsoa2.interfaces.PrimaryActivityContract;
import com.example.appsoa2.presenters.PrimaryPresenter;
import com.example.appsoa2.views.PrimaryActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PrimaryModel implements PrimaryActivityContract.ModelMVP {
    private static final String TAG = "PrimaryModel";

    private StringBuilder recDataString = new StringBuilder();
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private PrimaryModel.ConnectedThread mConnectedThread;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int MAX_VALUE_LIGHT_LEVEL = 90;
    private int MIN_VALUE_LIGHT_LEVEL = 10;
    private String START_SEND_MARK_SE = "9";
    private final String EOF_SEND_MARK_SE = "#";
    private String GET_CURRENT_LIGHT_LEVEL = "1";
    private String GET_FINAL_LIGHT_LEVEL = "2";
    private int START_INDEX = 0;
    private int NOT_FOUND_INDEX = -1;
    private boolean firstAccess = true;

    @Override
    public void getReadyBluetooth(PrimaryPresenter presenter) {
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothIn = bluetoothMessageHandler_PrimaryThread(presenter);
    }

    @Override
    public void reconnectBluetoothDevice(String macAddress) {
        btSocket = creationSocketByDevice(macAddress);
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        Log.i(TAG, "Thread iniciado  " + mConnectedThread);
        mConnectedThread.write(GET_FINAL_LIGHT_LEVEL + EOF_SEND_MARK_SE);
    }

    @Override
    public void sendLevelValueToDevice(int lightValue) {
        int lightResultValue = lightValue >= MAX_VALUE_LIGHT_LEVEL ? MAX_VALUE_LIGHT_LEVEL : Math.max(lightValue, MIN_VALUE_LIGHT_LEVEL);
        String lightLevelResult = String.valueOf(lightResultValue);
        Log.i(TAG, "Luminosidad enviada al SE: " + lightLevelResult);
        mConnectedThread.write(START_SEND_MARK_SE + lightLevelResult + EOF_SEND_MARK_SE);
    }

    @Override
    public void getCurrentLightLevel() {
        mConnectedThread.write(GET_CURRENT_LIGHT_LEVEL + EOF_SEND_MARK_SE);
    }

    @Override
    public void closeSocket() {
        try
        {
            btSocket.close();
        } catch (IOException e2) {
            Log.i(TAG, "Excepcion  " + e2);
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.i(TAG, "Error en obtener stream desde socket: " + e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, START_INDEX, bytes);
                    Log.i(TAG, "Read de buffer: " + readMessage);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.i(TAG, "Error en lectura de caracter recibido / buffer: " + e);
                    break;
                }
            }
        }

        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
                Log.i(TAG, "Write a SE con valor: " + input);
            } catch (IOException e) {
                Log.i(TAG, "Error al mandar datos al SE " + e);
            }
        }
    }

    private boolean isNumericOrEOF(String strNum) {
        if (strNum == null) {
            return false;
        }

        if (strNum.indexOf("#") > -1)
            return true;
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private Handler bluetoothMessageHandler_PrimaryThread(final PrimaryPresenter presenter) {
        @SuppressLint("HandlerLeak") Handler handlerObject = new Handler() {
            @SuppressLint("HandlerLeak")
            public void handleMessage(android.os.Message msg) {

                Log.i(TAG, "Se recibio un dato desde el SE " + msg.obj);

                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    boolean isNumber = isNumericOrEOF(readMessage);
                    if (isNumber) {
                        recDataString.append(readMessage);

                        int endOfLineIndex = recDataString.indexOf("#");
                        if (endOfLineIndex > NOT_FOUND_INDEX) {
                            String dataInPrint = recDataString.substring(START_INDEX, endOfLineIndex);
                            recDataString.delete(START_INDEX, recDataString.length());
                            int lightLevelToSet = Integer.parseInt(String.valueOf(dataInPrint));

                            if(firstAccess){
                                presenter.saveFinalLight(lightLevelToSet);
                                firstAccess = false;
                            } else{
                                presenter.saveCurrentLight(lightLevelToSet);
                            }
                        }
                    }
                }
            }
        };
        return handlerObject;

    }

    private BluetoothSocket creationSocketByDevice(String address) {
        BluetoothSocket socketResult = null;

        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            socketResult = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            socketResult.connect();
            Log.i("[BLUETOOTH]", "Connected to: " + device.getName());
        } catch (IOException e) {
            try {
                socketResult.close();
            } catch (IOException c) {
                Log.i(TAG, "Excepcion al cerrar socket:  " + c);
                return socketResult;
            }
        }

        return socketResult;
    }

}
