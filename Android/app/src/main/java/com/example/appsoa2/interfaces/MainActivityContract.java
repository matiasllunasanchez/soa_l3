package com.example.appsoa2.interfaces;

import android.content.Context;

import com.example.appsoa2.presenters.MainPresenter;

import java.util.List;

public interface MainActivityContract {
    interface ViewMVP {
        void showResultOnToast(String message);
        void showResultOnLabel(String message);
        void requestPermissionsToUser(List<String> listPermissionsNeeded);
        void closeLoadingDialog();
        void showLoadingDialog();
        void askBTPermissions();
        void disableButtons();
        void enableButtons();
    }

    interface ModelMVP {
        void getReadyBluetooth(Context mainActivity, MainActivityContract.ModelMVP.OnSendToPresenter presenter);
        void stopBluetoothDiscovery();
        void permissionsGrantedProcess();
        String getConnectedMacAddress();
        void onDestroyProcess();
        void onResumeProcess();
        void onPauseProcess();
        interface OnSendToPresenter {
            void showOnToast(String message);
            void showOnLabel(String message);
            void closeLoadingDialog();
            void showLoadingDialog();
            void askBTPermission();
            void requestPermissions(List<String> listPermissionsNeeded);
            void disableButtons();
            void enableButtons();
        }
    }

    interface PresenterMVP extends BasePresenter {
        void getReadyLogic(Context context);
        void permissionsGrantedProcess();
        String getConnectedDeviceAddress();
    }
}
