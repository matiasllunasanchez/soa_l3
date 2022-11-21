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
        this.presenter.getReadyLogic();
        this.initializeButtons();
        this.initializeLabels();
        this.initializeOthers();
        Log.i(TAG, "Paso al estado Createad");
    }

    private void initializeButtons() {
        this.btnSave = findViewById(R.id.button_primary_save);
        this.btnBack = findViewById(R.id.button_primary_back);
        this.btnRefresh = findViewById(R.id.button_primary_refresh);

        this.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lightValue = Integer.parseInt(String.valueOf(inputTextbox.getText()));
                // presenter.saveInputValue(lightValue);

                // Mando 9 y luego el valor del 0 al 100.
                // Falta limitar esto de 10 a 90
                presenter.sendLightLevelValue(lightValue);
                showToast("Luminosidad deseada enviada");
            }
        });

        this.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Click en BACK ");
                try {
                    Intent k = new Intent(PrimaryActivity.this, MainActivity.class);
                    startActivity(k);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        this.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.getCurrentLevelLight();
            }
        });

    }

    private void initializeLabels() {
        this.txtCurrentLightLevel = this.findViewById(R.id.text_primary_currentLightLevel);
    }

    private void initializeOthers() {
        this.seekBarValue = (SeekBar) this.findViewById(R.id.seekbar_primary_finalLightLevel);
        this.inputTextbox = (EditText) this.findViewById(R.id.input_primary_finalLightLevel);
        this.inputTextbox.setFilters(new InputFilter[]{new MinMaxFilter("0", "100")});
        this.lampImg = (ImageView) this.findViewById(R.id.image_primary_led);

        this.seekBarValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i(TAG, "Mientras cambia la barra" + progress);
                inputTextbox.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "Apenas arranca a cambiar la barra");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "Al terminar de cambiar la barra");
            }
        });

        this.inputTextbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "Antes de cambiar input");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.i(TAG, "Mientra cambia input" + i);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.i(TAG, "Despues de cambiar input" + editable);

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
        Log.i(TAG, "Se guardo valor de luz actual: " + value);
        this.txtCurrentLightLevel.setText("Porcentaje de luz: " + String.valueOf(value) + "%");
        this.setLampLevel(value);
    }

    @Override
    public void saveFinalLightLevel(int value) {
        Log.i(TAG, "Se guardo valor de luz deseada: " + value);
        this.inputTextbox.setText(String.valueOf(value));
        seekBarValue.setProgress(Integer.parseInt(String.valueOf(value)));
        this.setLampLevel(value);
    }


    @Override
    public void consoleLog(String label, String msg) {
        Log.i(TAG, label + msg);
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
        // this.presenter.onDestroyProcess();
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String macAddress = extras.getString("Direccion_Bluethoot");
        this.presenter.reconnectDevice(macAddress);
    }

}
