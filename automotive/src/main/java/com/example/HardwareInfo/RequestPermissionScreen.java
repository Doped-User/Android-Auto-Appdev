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

    public RequestPermissionScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        List<String> permissionsToRequest = getPermissionsToRequest();
        if (permissionsToRequest.isEmpty()) {
            return createPermissionsGrantedTemplate();
        } else {
            return createPermissionsRequiredTemplate(permissionsToRequest);
        }
    }

    private List<String> getPermissionsToRequest() {
        List<String> permissions = new ArrayList<>();
        String[] declaredPermissions;

        try {
            PackageInfo packageInfo = getCarContext().getPackageManager().getPackageInfo(
                    getCarContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            declaredPermissions = packageInfo.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            return permissions;
        }

        if (declaredPermissions != null) {
            for (String permission : declaredPermissions) {
                try {
                    CarAppPermission.checkHasPermission(getCarContext(), permission);
                } catch (SecurityException e) {
                    permissions.add(permission);
                }
            }
        }
        return permissions;
    }

    private Template createPermissionsGrantedTemplate() {
        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.permissions_granted_msg))
                .setHeaderAction(Action.BACK)
                .addAction(new Action.Builder()
                        .setTitle(getCarContext().getString(R.string.close_action_title))
                        .setOnClickListener(this::finish).build())
                .build();
    }

    private Template createPermissionsRequiredTemplate(List<String> permissions) {
        StringBuilder message = new StringBuilder()
                .append(getCarContext().getString(R.string.needs_access_msg_prefix));

        for (String permission : permissions) {
            message.append(permission).append("\n");
        }

        OnClickListener listener = ParkedOnlyOnClickListener.create(() ->
                getCarContext().requestPermissions(permissions, (approved, rejected) -> {
                if (!approved.isEmpty()) {
                    invalidate();
                }
                CarToast.makeText(getCarContext(),
                        String.format("Approved: %s Rejected: %s", approved, rejected),
                        CarToast.LENGTH_LONG).show();

                if (!getCarContext().getPackageManager().hasSystemFeature(FEATURE_AUTOMOTIVE)) {
                    CarToast.makeText(getCarContext(),
                            getCarContext().getString(R.string.phone_screen_permission_msg),
                            CarToast.LENGTH_LONG).show();
                }
        }));

        Action grantAccessAction = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.grant_access_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(listener)
                .build();

        return new LongMessageTemplate.Builder(message.toString().trim())
                .setTitle(getCarContext().getString(R.string.required_permissions_title))
                .addAction(grantAccessAction)
                .setHeaderAction(Action.BACK)
                .build();
    }
}