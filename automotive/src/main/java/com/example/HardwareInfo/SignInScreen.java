package com.example.HardwareInfo;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.Template;
import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;

public class SignInScreen extends Screen {
    private final String expectedPin = "2345";

    public SignInScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return getPinSignInTemplate();
    }

    private Template getPinSignInTemplate() {
        InputCallback pinInputCallback = new InputCallback() {
            @OptIn(markerClass = ExperimentalCarApi.class)
            @Override
            public void onInputSubmitted(@NonNull String input) {
                if (expectedPin.equals(input)) {
                    CarToast.makeText(getCarContext(), R.string.sign_in_success_toast, CarToast.LENGTH_LONG).show();
                    getCarContext().getCarService(ScreenManager.class).push(new HardwareInfoScreen(getCarContext()));
                } else {
                    CarToast.makeText(getCarContext(), R.string.invalid_pin_error_msg, CarToast.LENGTH_LONG).show();
                }
            }
        };

        InputSignInMethod pinSignInMethod = new InputSignInMethod.Builder(pinInputCallback)
                .setHint(getCarContext().getString(R.string.pin_hint))
                .setKeyboardType(InputSignInMethod.KEYBOARD_NUMBER)
                .build();

        return new SignInTemplate.Builder(pinSignInMethod)
                .setTitle(getCarContext().getString(R.string.sign_in_title))
                .setInstructions(getCarContext().getString(R.string.pin_sign_in_instruction))
                .setHeaderAction(Action.BACK)
                .build();
    }
}