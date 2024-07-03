package com.example.HardwareInfo;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;

import java.util.ArrayList;
import java.util.List;

public class RequestPermissionScreen extends Screen {

    private final Action mRefreshAction = new Action.Builder()
            .setTitle(getCarContext().getString(R.string.refresh_action_title))
            .setBackgroundColor(CarColor.BLUE)
            .setOnClickListener(this::invalidate)
            .build();

    public RequestPermissionScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        List<String> permissions = new ArrayList<>();
        String[] declaredPermissions;
        try {
            PackageInfo info = getCarContext().getPackageManager().getPackageInfo(
                    getCarContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            declaredPermissions = info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.package_not_found_error_msg))
                    .setHeaderAction(Action.BACK)
                    .addAction(mRefreshAction)
                    .build();
        }

        if (declaredPermissions != null) {
            for (String declaredPermission : declaredPermissions) {
                if (!declaredPermission.startsWith("androidx.car.app")) {
                    try {
                        CarAppPermission.checkHasPermission(getCarContext(), declaredPermission);
                    } catch (SecurityException e) {
                        permissions.add(declaredPermission);
                    }
                }
            }
        }

        if (permissions.isEmpty()) {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.permissions_granted_msg))
                    .setHeaderAction(Action.BACK)
                    .addAction(new Action.Builder()
                            .setTitle(getCarContext().getString(R.string.close_action_title))
                            .setOnClickListener(this::finish)
                            .build())
                    .build();
        }

        StringBuilder message = new StringBuilder()
                .append(getCarContext().getString(R.string.needs_access_msg_prefix));
        for (String permission : permissions) {
            message.append(permission).append("\n");
        }

        OnClickListener listener = ParkedOnlyOnClickListener.create(() -> {
            getCarContext().requestPermissions(permissions, (approved, rejected) ->
                    CarToast.makeText(getCarContext(),
                            String.format("Approved: %s Rejected: %s", approved, rejected),
                            CarToast.LENGTH_LONG).show());
            if (!getCarContext().getPackageManager().hasSystemFeature(FEATURE_AUTOMOTIVE)) {
                CarToast.makeText(getCarContext(),
                        getCarContext().getString(R.string.phone_screen_permission_msg),
                        CarToast.LENGTH_LONG).show();
            }
        });

        Action action = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.grant_access_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(listener)
                .build();

        return new LongMessageTemplate.Builder(message)
                .setTitle(getCarContext().getString(R.string.required_permissions_title))
                .addAction(action)
                .setHeaderAction(Action.BACK)
                .build();
    }
}