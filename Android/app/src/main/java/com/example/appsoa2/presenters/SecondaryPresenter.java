package com.example.appsoa2.presenters;

import android.content.Context;
import android.hardware.SensorEvent;

import com.example.appsoa2.interfaces.BasePresenter;
import com.example.appsoa2.interfaces.SecondaryActivityContract;
import com.example.appsoa2.models.SecondaryModel;
import com.example.appsoa2.views.SecondaryActivity;

public class SecondaryPresenter implements SecondaryActivityContract.ModelMVP.OnSendToPresenter, SecondaryActivityContract.PresenterMVP, BasePresenter {

    private SecondaryActivityContract.ViewMVP mainView;
    private final SecondaryActivityContract.ModelMVP model;

    public SecondaryPresenter(SecondaryActivityContract.ViewMVP mainView) {
        this.mainView = mainView;
        this.model = new SecondaryModel();
    }

    @Override
    public void shakeEventHandler() {
        this.model.generateColor(this);
    }

    @Override
    public void getReadyLogic(Context context) {
        this.model.getReadySensors(context);
        this.model.getReadyBluetooth(context);
    }

    @Override
    public void movementDetected(SensorEvent sensorEvent) {
        this.model.movementDetected(sensorEvent, this);
    }

    @Override
    public void safeDisconnect(SecondaryActivity secondaryActivity) {
        this.model.disconnectBT();
        this.model.disconnectSensors(secondaryActivity);
    }

    @Override
    public void getReadyLogicAgain(SecondaryActivity secondaryActivity) {
        this.model.reconnectSensors(secondaryActivity);
    }

    @Override
    public void reconnectDevice(String address) {
        this.model.reconnectBluetoothDevice(address);
    }

    @Override
    public void sendColorToDevice(String valueOf) {
        this.model.sendLedColorValue(valueOf);
    }

    @Override
    public void handleShakerResult(int value, int codeColor) {
        String hexColor = String.format("#%06X", (0xFFFFFF & value));
        this.mainView.setCurrentColor(value, hexColor, codeColor);
    }

    @Override
    public void onCreatedProcess() {

    }

    @Override
    public void onStartProcess() {

    }

    @Override
    public void onResumeProcess() {

    }

    @Override
    public void onPauseProcess() {

    }

    @Override
    public void onStopProcess() {

    }

    @Override
    public void onRestartProcess() {

    }

    @Override
    public void onDestroyProcess() {
        this.mainView = null;
        this.model.closeSocket();
    }

}

