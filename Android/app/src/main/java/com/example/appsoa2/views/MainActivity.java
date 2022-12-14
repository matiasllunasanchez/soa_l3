package com.example.appsoa2.views;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import android.app.Activity;
import android.app.ProgressDialog;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.example.appsoa2.interfaces.MainActivityContract;
import com.example.appsoa2.presenters.MainPresenter;

import net.londatiga.android.bluetooth.R;

public class MainActivity extends Activity implements MainActivityContract.ViewMVP {

    private MainActivityContract.PresenterMVP presenter;
    private static final String TAG = "MainActivity";
    private TextView txtEstado;
    private ProgressDialog mProgressDlg;
    Button btnPrimary;
    Button btnSecondary;

    public static final int MULTIPLE_PERMISSIONS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setContentView(R.layout.activity_main);
        this.initialize();
    }

    private void initialize() {
        initializeButtons();
        initializeRest();
        this.presenter.getReadyLogic(this);
    }

    private void initializeButtons(){
        btnPrimary = findViewById(R.id.button_primary);
        btnSecondary = findViewById(R.id.button_secondary);

        btnPrimary.setOnClickListener(this.btnListener);
        btnSecondary.setOnClickListener(this.btnListener);
        this.presenter = new MainPresenter(this);
        this.txtEstado = (TextView) findViewById(R.id.txtEstado);
    }

    private void initializeRest(){
        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Buscando dispositivos...");
        mProgressDlg.setCancelable(false);
    }

    private View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String address = presenter.getConnectedDeviceAddress();
            switch (view.getId()) {
                case R.id.button_primary:
                    consoleLog("Ir a pantalla primaria / ILUMINACION","");
                    try {
                        if (address != null) {
                            btnPrimary.setEnabled(false);
                            btnSecondary.setEnabled(false);
                            txtEstado.setText("Esper??.... Cargando pantalla de iluminaci??n...");
                            Intent k = new Intent(MainActivity.this, PrimaryActivity.class);
                            k.putExtra("Direccion_Bluethoot", address);
                            startActivity(k);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        consoleLog("Excepcion al querer ir a la pantalla de iluminacion", e.toString());
                    }
                    break;
                case R.id.button_secondary:
                    consoleLog("Ir a pantalla secundaria / COLOR","");
                    try {
                        if (address != null) {
                            btnSecondary.setEnabled(false);
                            btnPrimary.setEnabled(false);
                            txtEstado.setText("Esper??.... Cargando pantalla de color...");
                            Intent k = new Intent(MainActivity.this, SecondaryActivity.class);
                            k.putExtra("Direccion_Bluethoot", address);
                            startActivity(k);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        consoleLog("Excepcion al querer ir a la pantalla de color", e.toString());
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value " + view.getId());
            }
        }
    };

    @Override
    public void onPause() {
        this.presenter.onPauseProcess();
        super.onPause();
    }

    @Override
    public void onResume() {
        this.presenter.onResumeProcess();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        this.presenter.onDestroyProcess();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.presenter.permissionsGrantedProcess();
                } else {
                    String perStr = "";
                    for (String per : permissions) {
                        perStr += "\n" + per;
                    }
                    Toast.makeText(this, "Esta aplicaci??n requiere de la aceptaci??n de todos los permisos para funcionar correctamente.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void requestPermissionsToUser(List<String> listPermissionsNeeded) {
        ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
    }

    @Override
    public void askBTPermissions() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1000);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showResultOnToast(String msg) {
        consoleLog("Mostrar en toast:", msg);
        showToast(msg);
    }

    @Override
    public void showResultOnLabel(String msg) {
        consoleLog("Mostrar en label:", msg);
        this.txtEstado.setText(msg);
    }

    @Override
    public void closeLoadingDialog() {
        mProgressDlg.dismiss();
    }

    @Override
    public void showLoadingDialog() {
        mProgressDlg.show();
    }

    private void consoleLog(String label, String data) {
        Log.i(TAG, label +" "+ data);
    }

    @Override
    public void disableButtons() {
        btnPrimary.setEnabled(false);
        btnSecondary.setEnabled(false);
    }

    @Override
    public void enableButtons() {
        btnPrimary.setEnabled(true);
        btnSecondary.setEnabled(true);
    }
}

