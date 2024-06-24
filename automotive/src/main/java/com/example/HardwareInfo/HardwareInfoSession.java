package com.example.HardwareInfo;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

public final class HardwareInfoSession extends Session {
    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        return new HardwareInfoScreen(getCarContext());
    }
}
