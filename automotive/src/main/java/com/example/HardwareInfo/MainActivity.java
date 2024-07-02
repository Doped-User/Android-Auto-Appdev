package com.example.HardwareInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView carDataTextView;
    private final BroadcastReceiver carDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.HardwareInfo.SEND_CAR_DATA".equals(intent.getAction())) {
                StringBuilder carInfoBuilder = new StringBuilder();
                appendData(carInfoBuilder, intent, "MODEL_INFO", "Model Info");
                appendData(carInfoBuilder, intent, "ENERGY_PROFILE_INFO", "Energy Profile Info");
                appendData(carInfoBuilder, intent, "SPEED_INFO", "Speed Info");
                appendData(carInfoBuilder, intent, "ENERGY_LEVEL_INFO", "Energy Level Info");
                appendData(carInfoBuilder, intent, "TOLL_CARD_INFO", "Toll Card Status");
                appendData(carInfoBuilder, intent, "MILEAGE_INFO", "Mileage Info");
                appendData(carInfoBuilder, intent, "ACCELEROMETER_INFO", "Accelerometer Info");
                appendData(carInfoBuilder, intent, "GYROSCOPE_INFO", "Gyroscope Info");
                appendData(carInfoBuilder, intent, "COMPASS_INFO", "Compass Info");
                appendData(carInfoBuilder, intent, "CAR_LOCATION_INFO", "Car Location Info");

                Log.d("MainActivity", "Car data received: " + carInfoBuilder);
                carDataTextView.setText(carInfoBuilder.toString());
            }
        }
    };

    private void appendData(StringBuilder builder, Intent intent, String key, String label) {
        String data = intent.getStringExtra(key);
        if (key.equals("TOLL_CARD_INFO")) {
            String tollCardState = data.equals("1") ? "Inserted" : "Not Inserted";
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