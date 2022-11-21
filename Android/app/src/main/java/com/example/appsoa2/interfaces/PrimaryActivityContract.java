package com.example.appsoa2.interfaces;

import com.example.appsoa2.presenters.PrimaryPresenter;

public interface PrimaryActivityContract {
    interface ViewMVP {
        void consoleLog(String label, String msg);
        void saveCurrentLightLevel(int i);
        void saveFinalLightLevel(int i);
    }

    interface ModelMVP {
        void getReadyBluetooth(PrimaryPresenter presenter);
        void reconnectBluetoothDevice(String macAddress);
        void sendLevelValueToDevice(int lightValue);
        void getCurrentLightLevel();
        void closeSocket();

         interface OnSendToPresenter {
        }
    }

    interface PresenterMVP extends BasePresenter {
        void saveCurrentLight(int i);
        void saveFinalLight(int i);
        void getReadyLogic();
        void reconnectDevice(String macAddress);
        void sendLightLevelValue(int lightValue);
        void getCurrentLevelLight();
        void onPauseProcess();
    }
}
