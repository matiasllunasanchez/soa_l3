package com.example.appsoa2.models;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.example.appsoa2.interfaces.MainActivityContract;
import com.example.appsoa2.interfaces.SecondaryActivityContract;
import com.example.appsoa2.presenters.SecondaryPresenter;
import com.example.appsoa2.views.SecondaryActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.Random;
import java.util.UUID;

public class SecondaryModel implements SecondaryActivityContract.ModelMVP {
    private static final String TAG = "SecondaryModel";
    public static final int ALPHA = 255;
    public static final int FULL_VALUE_COLOR = 255;
    public static final int EMPTY_VALUE_COLOR = 0;
    public static final String SENSOR_SERVICE = "sensor";
    private final int RED_COLOR = 3;
    private final int GREEN_COLOR = 4;
    private final int BLUE_COLOR = 5;
    private final int WHITE_COLOR = 6;
    private final int RANDOM_COLOR_OPERATION_VALUE = 4;
    private final int RANDOM_COLOR_OPERATION_VALUE_OFFSET = 3;
    private final String EOF_SE = "#";

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private static final float SHAKE_THRESHOLD = 5f;
    private float currentPositionX, currentPositionY, currentPositionZ, lastPositionX, lastPositionY, lastPositionZ;
    private boolean notFirstMove = false;
    private float xDiff, yDiff, zDiff;
    private Vibrator vibratorObj;
    private final int VIBRATOR_DURATION = 500;

    private SecondaryModel.ConnectedThread mConnectedThread;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private SecondaryPresenter currentPresenter;
    private Object pauseLock;
    private boolean paused;
    private boolean finished;

    public SecondaryModel(){
        this.pauseLock = new Object();
        this.paused = false;
        this.finished = false;
    }

    @Override
    public void getReadySensors(Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        this.sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.vibratorObj = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void movementDetected(SensorEvent sensorEvent, SecondaryActivityContract.ModelMVP.OnSendToPresenter presenter) {
        currentPositionX = sensorEvent.values[0];
        currentPositionY = sensorEvent.values[1];
        currentPositionZ = sensorEvent.values[2];

        if (notFirstMove) {
            xDiff = Math.abs(lastPositionX - currentPositionX);
            yDiff = Math.abs(lastPositionY - currentPositionY);
            zDiff = Math.abs(lastPositionZ - currentPositionZ);
            if ((xDiff > SHAKE_THRESHOLD && yDiff > SHAKE_THRESHOLD) || (yDiff > SHAKE_THRESHOLD && zDiff > SHAKE_THRESHOLD) || (xDiff > SHAKE_THRESHOLD && zDiff > SHAKE_THRESHOLD)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibratorObj.vibrate(VibrationEffect.createOneShot(VIBRATOR_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibratorObj.vibrate(VIBRATOR_DURATION);
                }
                generateColor(presenter);
            }
        }
        lastPositionX = currentPositionX;
        lastPositionY = currentPositionY;
        lastPositionZ = currentPositionZ;
        notFirstMove = true;
    }

    @Override
    public void disconnectBT() {
        try {
            btSocket.close();
        } catch (IOException e2) {
            consoleLog("Excepcion al intentar cerrar socket de BT", e2.toString());
        }
    }

    @Override
    public void disconnectSensors(Context context) {
        this.sensorManager.unregisterListener((SensorEventListener) context);
    }

    @Override
    public void connectSensors(Context context) {
        consoleLog("Intenta reconectar sensores","");
        this.sensorManager.registerListener((SensorEventListener) context, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void getReadyBluetooth(SecondaryPresenter presenter) {
        this.currentPresenter = presenter;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void connectBluetoothDevice(String address) {
        btSocket = creationSocketByDevice(address);
        mConnectedThread = new SecondaryModel.ConnectedThread(btSocket);
        mConnectedThread.start();
        mConnectedThread.write(String.valueOf(WHITE_COLOR) + EOF_SE);
    }

    @Override
    public void sendLedColorValue(String valueOf) {
        this.mConnectedThread.write(valueOf + EOF_SE);
    }

    @Override
    public void generateColor(SecondaryActivityContract.ModelMVP.OnSendToPresenter presenter) {
        int newColor = new Random().nextInt(RANDOM_COLOR_OPERATION_VALUE) + RANDOM_COLOR_OPERATION_VALUE_OFFSET;
        int resultColor = 0;
        int codeColor = WHITE_COLOR;
        switch (newColor) {
            case RED_COLOR:
                resultColor = Color.argb(ALPHA, FULL_VALUE_COLOR, EMPTY_VALUE_COLOR, EMPTY_VALUE_COLOR);
                codeColor = RED_COLOR;
                break;
            case GREEN_COLOR:
                resultColor = Color.argb(ALPHA, EMPTY_VALUE_COLOR, FULL_VALUE_COLOR, EMPTY_VALUE_COLOR);
                codeColor = GREEN_COLOR;
                break;
            case BLUE_COLOR:
                resultColor = Color.argb(ALPHA, EMPTY_VALUE_COLOR, EMPTY_VALUE_COLOR, FULL_VALUE_COLOR);
                codeColor = BLUE_COLOR;
                break;
            case WHITE_COLOR:
                resultColor = Color.argb(ALPHA, FULL_VALUE_COLOR, FULL_VALUE_COLOR, FULL_VALUE_COLOR);
                break;
            default:
                resultColor = Color.argb(ALPHA, EMPTY_VALUE_COLOR, EMPTY_VALUE_COLOR, EMPTY_VALUE_COLOR);
        }
        presenter.handleShakerResult(resultColor, codeColor);
    }

    private class ConnectedThread extends Thread {
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                consoleLog("Excepcion al obtener lectura del SE:", e.toString());
            }
            mmOutStream = tmpOut;
        }
        public void run() {
                synchronized (pauseLock) {
                    while (paused) {
                        try {
                            pauseLock.wait();
                        } catch (
                                InterruptedException e) {
                            break;
                        }
                    }
                }
        }

        public void write(String input) {
            Log.i(TAG, "Write con color: " + input);
            byte[] msgBuffer = input.getBytes();

            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                consoleLog("Excepcion al enviar write al SE:", e.toString());
                currentPresenter.showOnToast("Error en env??o de datos al SE");
            }
        }
    }

    private BluetoothSocket creationSocketByDevice(String address) {
        BluetoothSocket socketResult = null;
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        consoleLog("La address recibida",address);
        try {
            socketResult = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            socketResult.connect();
            consoleLog("[BLUETOOTH] conectado a:", device.getName());
        } catch (IOException e) {
            consoleLog("Excepcion al conectar el socket:", e.toString());
            try {
                socketResult.close();
            } catch (IOException c) {
                consoleLog("Excepcion al cerrar socket:", c.toString());
                return socketResult;
            }
        }
        return socketResult;
    }

    @Override
    public void closeSocket() {
        try
        {
            btSocket.close();
        } catch (IOException e2) {
            consoleLog("Excepcion al cerrar socket:", e2.toString());
        }
    }

    private void consoleLog(String label, String data) {
        Log.i(TAG, label + data);
    }

       @Override
    public void unpauseThread() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    @Override
    public void pauseThread() {
        synchronized (pauseLock) {
            paused = true;
        }
    }

    @Override
    public void closeThread() {
        finished = true;
    }
}
