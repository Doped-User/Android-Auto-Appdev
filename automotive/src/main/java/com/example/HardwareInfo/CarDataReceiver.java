package com.example.HardwareInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CarDataReceiver extends BroadcastReceiver {

    private static final String TAG = "CarDataReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.HardwareInfo.SEND_CAR_DATA".equals(intent.getAction())) {
            String carData = intent.getStringExtra("car_data");
            Log.d(TAG, "Received car data: " + carData);
            Intent displayIntent = new Intent(context, MainActivity.class);
            displayIntent.putExtra("car_data", carData);
            displayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(displayIntent);
        }
    }
}