package com.example.appsoa2.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import net.londatiga.android.bluetooth.R;

import com.example.appsoa2.interfaces.PrimaryActivityContract;
import com.example.appsoa2.presenters.PrimaryPresenter;
import com.example.appsoa2.views.components.MinMaxFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.UUID;

public class PrimaryActivity extends Activity implements PrimaryActivityContract.ViewMVP {
    private static final String TAG = "PrimaryActivity";
    private PrimaryActivityContract.PresenterMVP presenter;
    private static final int MIN_LIGHT_VALUE = 30;
    private static final int EMPTY_LIGHT_VALUE = 0;
    private static final int MEDIUM_LIGHT_VALUE = 65;

    private Button btnSave, btnBack, btnRefresh;
    private TextView txtCurrentLightLevel;
    private TextView txtFinalLightLevel;
    private EditText inputTextbox;
    private SeekBar seekBarValue;
    private ImageView lampImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_primary);
        this.initialize();
    }

    private void initialize() {
        this.presenter = new PrimaryPresenter(this);
        this.initializeButtons();
        this.initializeLabels();
        this.initializeRest();
        this.presenter.getReadyLogic();
    }

    private void initializeButtons() {
        this.btnSave = findViewById(R.id.button_primary_save);
        this.btnBack = findViewById(R.id.button_primary_back);
        this.btnRefresh = findViewById(R.id.button_primary_refresh);

        this.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lightValue = Integer.parseInt(String.valueOf(inputTextbox.getText()));
                presenter.sendLightLevelValue(lightValue);
                showToast("Luminosidad deseada enviada: "+ String.valueOf(lightValue)+"%");
            }
        });

        this.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Click en BACK ");
                try {
                    disableButtons();
                    Intent k = new Intent(PrimaryActivity.this, MainActivity.class);
                    startActivity(k);
                } catch (Exception e) {
                    e.printStackTrace();
                    consoleLog("Excepcion al clickear en volver:", e.toString());
                }
            }
        });

        this.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.getCurrentLevelLight();
            }
        });
        disableButtons();
    }

    private void initializeLabels() {
        this.txtCurrentLightLevel = this.findViewById(R.id.text_primary_currentLightLevel);
        this.txtFinalLightLevel = this.findViewById(R.id.textView5);
    }

    private void initializeRest() {
        this.seekBarValue = (SeekBar) this.findViewById(R.id.seekbar_primary_finalLightLevel);
        this.inputTextbox = (EditText) this.findViewById(R.id.input_primary_finalLightLevel);
        this.inputTextbox.setFilters(new InputFilter[]{new MinMaxFilter("0", "100")});
        this.lampImg = (ImageView) this.findViewById(R.id.image_primary_led);

        this.seekBarValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                inputTextbox.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        this.inputTextbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(""))
                    seekBarValue.setProgress(0);
                else {
                    seekBarValue.setProgress(Integer.parseInt(String.valueOf(editable)));
                }
            }
        });
    }

    @Override
    public void saveCurrentLightLevel(int value) {
        this.txtCurrentLightLevel.setText("Porcentaje de luz: " + String.valueOf(value) + "%");
        this.setLampLevel(value);
        enableButtons();
    }

    @Override
    public void saveFinalLightLevel(int value) {
        this.inputTextbox.setText(String.valueOf(value));
        seekBarValue.setProgress(Integer.parseInt(String.valueOf(value)));
        this.setLampLevel(value);
        this.txtFinalLightLevel.setText("Luminosidad deseada:");
        enableButtons();
    }

    private void setLampLevel(int value) {
        if (value > EMPTY_LIGHT_VALUE) {
            if (value > MIN_LIGHT_VALUE) {
                if (value > MEDIUM_LIGHT_VALUE) {
                    this.lampImg.setImageResource(R.drawable.lamp1_full);
                } else {
                    this.lampImg.setImageResource(R.drawable.lamp1_med);
                }
            } else {
                this.lampImg.setImageResource(R.drawable.lamp1_min);
            }
        } else {
            this.lampImg.setImageResource(R.drawable.lamp_values);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.presenter.onPauseProcess();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.presenter.onDestroyProcess();
    }

    @Override
    public void onResume() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String macAddress = extras.getString("Direccion_Bluethoot");
        this.presenter.reconnectDevice(macAddress);
        super.onResume();
    }

    private void consoleLog(String label, String data) {
        Log.i(TAG, label +" "+ data);
    }

    private void enableButtons(){
        this.btnSave.setEnabled(true);
        this.btnBack.setEnabled(true);
    }

    private void disableButtons(){
        this.btnSave.setEnabled(false);
        this.btnBack.setEnabled(false);
    }
}
