package com.example.appsoa2.interfaces;

import android.content.Context;
import android.hardware.SensorEvent;

import com.example.appsoa2.presenters.SecondaryPresenter;
import com.example.appsoa2.views.SecondaryActivity;

public interface SecondaryActivityContract {
    interface ViewMVP {
        void setCurrentColor(int value, String hexColor, int codeColor);
        void consoleLog(String label, String msg);
        void showResultOnToast(String message);
    }

    interface ModelMVP {
        void getReadySensors(Context context);
        void movementDetected(SensorEvent sensorEvent, SecondaryActivityContract.ModelMVP.OnSendToPresenter presenter);
        void disconnectBT();
        void disconnectSensors(Context context);
        void connectSensors(Context context);
        void getReadyBluetooth(SecondaryPresenter presenter);
        void connectBluetoothDevice(String address);
        void sendLedColorValue(String valueOf);
        void closeSocket();
        void generateColor(OnSendToPresenter presenter);

        interface OnSendToPresenter {
            void handleShakerResult(int resultColor, int value);
            void showOnToast(String message);
        }
    }

    interface PresenterMVP extends BasePresenter {
        void shakeEventHandler();
        void getReadyLogic(Context context);
        void movementDetected(SensorEvent sensorEvent);
        void safeDisconnect(Context context);
        void getReadyLogicAgain(Context context);
        void connectDevice(String address);
        void sendColorToDevice(String valueOf);
        void onDestroyProcess();
    }
}
