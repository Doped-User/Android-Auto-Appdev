package com.example.HardwareInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView carDataTextView;

    private final BroadcastReceiver carDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.HardwareInfo.SEND_CAR_DATA".equals(intent.getAction())) {
                String carData = intent.getStringExtra("car_data");
                if (carData != null) {
                    Log.d("MainActivity", "Car data received: " + carData);
                    carDataTextView.setText(carData);
                } else {
                    Log.d("MainActivity", "No car data received");
                    carDataTextView.setText("No car data received");
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        carDataTextView = findViewById(R.id.carDataTextView);
        Button btn = findViewById(R.id.btnShow);

        btn.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Requesting Car Data", Toast.LENGTH_SHORT).show();
            requestCarData();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.example.HardwareInfo.SEND_CAR_DATA");
        registerReceiver(carDataReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(carDataReceiver);
    }

    private void requestCarData() {
        Intent intent = new Intent("com.example.HardwareInfo.REQUEST_CAR_DATA");
        sendBroadcast(intent);
    }
}
