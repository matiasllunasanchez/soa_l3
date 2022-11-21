package com.example.appsoa2.interfaces;

public interface BasePresenter {
    void onCreatedProcess();
    void onStartProcess();
    void onResumeProcess();
    void onPauseProcess();
    void onStopProcess();
    void onRestartProcess();
    void onDestroyProcess();
    void consoleLog(String label, String msg);
}
