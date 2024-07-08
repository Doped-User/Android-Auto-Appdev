package com.example.HardwareInfo;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.annotations.ExperimentalCarApi;

public final class HardwareInfoSession extends Session {
    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        return new HardwareInfoScreen(getCarContext());
    }
}
