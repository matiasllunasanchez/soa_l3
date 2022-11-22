package com.example.appsoa2.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.londatiga.android.bluetooth.R;

import com.example.appsoa2.interfaces.SecondaryActivityContract;
import com.example.appsoa2.presenters.SecondaryPresenter;

public class SecondaryActivity extends Activity implements SecondaryActivityContract.ViewMVP, SensorEventListener {
    private static final String TAG = "SecondaryActivity";
    private SecondaryActivityContract.PresenterMVP presenter;

    private Button btnBack;
    private TextView txtColorSelected;
    private static final String RED_COLOR_HEX = "#FF0000";
    private static final String GREEN_COLOR_HEX = "#00FF00";
    private static final String BLUE_COLOR_HEX = "0000FF";
    private static final String WHITE_COLOR_HEX = "FFFFFF";
    private ImageView lampImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_secondary);
        this.initialize();
    }

    private void initialize() {
        this.presenter = new SecondaryPresenter(this);
        this.initializeButtons();
        this.initializeRest();
        this.presenter.getReadyLogic(this);
    }

    private void initializeRest() {
        this.txtColorSelected = this.findViewById(R.id.text_ledColorSelected);
        this.txtColorSelected.setText("-");
        this.lampImg = (ImageView) this.findViewById(R.id.image_secondary_led);
    }

    private void initializeButtons() {
        this.btnBack = this.findViewById(R.id.button_secondary_back);
        this.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btnBack.setEnabled(false);
                    Intent k = new Intent(SecondaryActivity.this, MainActivity.class);
                    startActivity(k);
                } catch (Exception e) {
                    consoleLog("Excepcion al presionar volver:", e.toString());
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void setCurrentColor(int value, String hexColor, int codeColor) {
        this.txtColorSelected.setText(hexColor);
        this.setLampColor(hexColor);
        this.lampImg.setColorFilter(value, PorterDuff.Mode.SRC_ATOP);
        Log.i(TAG, "Color a mandar al SE " + codeColor);
        this.presenter.sendColorToDevice(String.valueOf(codeColor));
        showToast("¡Cambió el color del led!");
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        this.presenter.movementDetected(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setLampColor(String value) {
        switch (value) {
            case RED_COLOR_HEX:
                this.lampImg.setImageResource(R.drawable.lamp_red);
                break;
            case GREEN_COLOR_HEX:
                this.lampImg.setImageResource(R.drawable.lamp_green);
                break;
            case BLUE_COLOR_HEX:
                this.lampImg.setImageResource(R.drawable.lamp_blue);
                break;
            case WHITE_COLOR_HEX:
                this.lampImg.setImageResource(R.drawable.lamp_white);
                break;
            default:
                this.lampImg.setImageResource(R.drawable.lamp_values);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.presenter.onPauseProcess();
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String address = extras.getString("Direccion_Bluethoot");
        consoleLog("Reconecto dispositivo y seteo color de LED","");
        this.presenter.connectDevice(address);
        this.presenter.onResumeProcess();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.presenter.onDestroyProcess();
    }

    @Override
    public void consoleLog(String label, String msg) {
        Log.i(TAG, label + msg);
    }

    public void showResultOnToast(String msg) {
        consoleLog("Mostrar en toast:", msg);
        showToast(msg);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
