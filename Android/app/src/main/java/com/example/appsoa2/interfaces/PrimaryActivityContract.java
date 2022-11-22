package com.example.appsoa2.interfaces;

import com.example.appsoa2.presenters.PrimaryPresenter;

public interface PrimaryActivityContract {
    interface ViewMVP {
        void saveCurrentLightLevel(int i);
        void saveFinalLightLevel(int i);
        void showResultOnToast(String message);
    }

    interface ModelMVP {
        void getReadyBluetooth(PrimaryPresenter presenter);
        void connectBluetoothDevice(String macAddress);
        void sendLevelValueToDevice(int lightValue);
        void getCurrentLightLevel();
        void closeSocket();
        void unpauseThread();
        void pauseThread();
        void closeThread();

        interface OnSendToPresenter {
             void showOnToast(String message);
        }
    }

    interface PresenterMVP extends BasePresenter {
        void saveCurrentLight(int i);
        void saveFinalLight(int i);
        void getReadyLogic();
        void connectDevice(String macAddress);
        void sendLightLevelValue(int lightValue);
        void getCurrentLevelLight();
        void onPauseProcess();
    }
}
