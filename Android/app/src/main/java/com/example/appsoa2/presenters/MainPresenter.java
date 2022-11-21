package com.example.appsoa2.presenters;

import android.content.Context;

import com.example.appsoa2.interfaces.BasePresenter;
import com.example.appsoa2.interfaces.MainActivityContract;
import com.example.appsoa2.models.MainModel;

import java.util.List;

public class MainPresenter implements MainActivityContract.ModelMVP.OnSendToPresenter, MainActivityContract.PresenterMVP, BasePresenter {

    private MainActivityContract.ViewMVP mainView;
    private final MainActivityContract.ModelMVP model;

    public MainPresenter(MainActivityContract.ViewMVP mainView) {
        this.mainView = mainView;
        this.model = new MainModel();
    }

    @Override
    public void getReadyLogic(Context currentContext) {
        this.model.getReadyBluetooth(currentContext, this);
    }

    @Override
    public void onCreatedProcess() { }

    @Override
    public void onStartProcess() { }

    @Override
    public void onStopProcess() { }

    @Override
    public void onRestartProcess() { }

    @Override
    public void onResumeProcess() {
        this.model.onResumeProcess();
    }

    @Override
    public void onDestroyProcess() {
        this.model.onDestroyProcess();
        this.mainView = null;
    }

    @Override
    public void disableButtons() {
        this.mainView.disableButtons();
    }

    @Override
    public void enableButtons() {
        this.mainView.enableButtons();
    }

    @Override
    public void onPauseProcess() {
        this.model.onPauseProcess();
    }

    @Override
    public void permissionsGrantedProcess() {
        this.model.permissionsGrantedProcess();
    }

    @Override
    public String getConnectedDeviceAddress() {
        return this.model.getConnectedMacAddress();
    }

    @Override
    public void showOnToast(String message) {
        this.mainView.showResultOnToast(message);
    }

    @Override
    public void showOnLabel(String message) {
        this.mainView.showResultOnLabel(message);
    }

    @Override
    public void closeLoadingDialog() {
        this.mainView.closeLoadingDialog();
    }

    @Override
    public void showLoadingDialog() {
        this.mainView.showLoadingDialog();
    }

    @Override
    public void askBTPermission() {
        this.mainView.askBTPermissions();
    }

    public void requestPermissions(List<String> listPermissionsNeeded) {
        this.mainView.requestPermissionsToUser(listPermissionsNeeded);
    }
}



