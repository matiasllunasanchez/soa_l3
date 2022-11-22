package com.example.appsoa2.presenters;

import com.example.appsoa2.interfaces.BasePresenter;
import com.example.appsoa2.interfaces.PrimaryActivityContract;
import com.example.appsoa2.models.PrimaryModel;

public class PrimaryPresenter implements PrimaryActivityContract.ModelMVP.OnSendToPresenter, PrimaryActivityContract.PresenterMVP {

    private PrimaryActivityContract.ViewMVP mainView;
    private final PrimaryActivityContract.ModelMVP model;

    public PrimaryPresenter(PrimaryActivityContract.ViewMVP mainView) {
        this.mainView = mainView;
        this.model = new PrimaryModel();
    }

    @Override
    public void saveCurrentLight(int i) {
        this.mainView.saveCurrentLightLevel( i);
    }

    @Override
    public void saveFinalLight(int i) {
        this.mainView.saveFinalLightLevel(i);
    }

    @Override
    public void getReadyLogic() {
        this.model.getReadyBluetooth(this);
    }

    @Override
    public void connectDevice(String macAddress) {
        this.model.connectBluetoothDevice(macAddress);
    }

    @Override
    public void sendLightLevelValue(int lightValue) {
        this.model.sendLevelValueToDevice(lightValue);
    }

    @Override
    public void getCurrentLevelLight() {
        this.model.getCurrentLightLevel();
    }

    @Override
    public void onCreatedProcess() {

    }

    @Override
    public void onStartProcess() {

    }

    @Override
    public void onResumeProcess() {
        this.model.unpauseThread();
    }

    @Override
    public void onPauseProcess() {
        this.model.pauseThread();
        this.model.closeSocket();
    }

    @Override
    public void onStopProcess() {
        this.model.closeThread();
        this.model.closeSocket();
    }

    @Override
    public void onRestartProcess() {

    }

    @Override
    public void onDestroyProcess() {
        this.mainView = null;
        this.model.closeSocket();
    }

    @Override
    public void showOnToast(String message) {
        this.mainView.showResultOnToast(message);
    }
}

