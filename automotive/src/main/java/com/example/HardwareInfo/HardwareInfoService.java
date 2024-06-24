package com.example.HardwareInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;
import android.content.Context;
import android.content.Intent;

public final class HardwareInfoService extends CarAppService {
    private static final String CAR_DATA_ACTION = "com.example.HardwareInfo.CAR_DATA_ACTION";

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return new HardwareInfoSession();
    }

    // Method to broadcast car data
    public static void broadcastCarData(String carData, Context context) {
        Intent intent = new Intent(CAR_DATA_ACTION);
        intent.putExtra("car_data", carData);
        context.sendBroadcast(intent);
    }
}
