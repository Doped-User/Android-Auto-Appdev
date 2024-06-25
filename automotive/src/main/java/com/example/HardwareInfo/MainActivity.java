package com.example.HardwareInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView carDataTextView;
    private final BroadcastReceiver carDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.HardwareInfo.SEND_CAR_DATA".equals(intent.getAction())) {
                String carData = intent.getStringExtra("car_data");
                carDataTextView.setText(carData);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        carDataTextView = findViewById(R.id.carDataTextView);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(carDataReceiver, new IntentFilter("com.example.HardwareInfo.SEND_CAR_DATA"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(carDataReceiver);
    }
}