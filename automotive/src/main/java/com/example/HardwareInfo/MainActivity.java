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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView carDataTextView;
    private final BroadcastReceiver carDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.HardwareInfo.SEND_CAR_DATA".equals(intent.getAction())) {
                StringBuilder carInfoBuilder = new StringBuilder();
                appendData(carInfoBuilder, intent, "MODEL_INFO", "Model");
                appendData(carInfoBuilder, intent, "ENERGY_PROFILE_INFO", "Energy Profile");
                appendData(carInfoBuilder, intent, "SPEED_INFO", "Speed");
                appendData(carInfoBuilder, intent, "ENERGY_LEVEL_INFO", "Energy Level");
                appendData(carInfoBuilder, intent, "TOLL_CARD_INFO", "Toll Card");
                appendData(carInfoBuilder, intent, "MILEAGE_INFO", "Mileage");
                appendData(carInfoBuilder, intent, "ACCELEROMETER_INFO", "Accelerometer");
                appendData(carInfoBuilder, intent, "GYROSCOPE_INFO", "Gyroscope");
                appendData(carInfoBuilder, intent, "COMPASS_INFO", "Compass");
                appendData(carInfoBuilder, intent, "CAR_LOCATION_INFO", "Car Location");

                Log.d("MainActivity", "Car data received: " + carInfoBuilder);
                carDataTextView.setText(carInfoBuilder.toString());
            }
        }
    };

    private void appendData(StringBuilder builder, @NonNull Intent intent, String key, String label) {
        String data = intent.getStringExtra(key);
        if (key.equals("TOLL_CARD_INFO")) {
            String tollCardState = data.equals("1") ? "\n\tToll Card Inserted" : (data.equals("2") ? "\n\tToll Card Not Inserted": "\n\tToll Card State Not Available");
            builder.append(label).append(": ").append(tollCardState).append("\n\n");
        } else {
            if (data != null) {
                builder.append(label).append(": ").append(data).append("\n\n");
            } else {
                builder.append("No ").append(label).append(" received\n\n");
            }
        }
    }

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