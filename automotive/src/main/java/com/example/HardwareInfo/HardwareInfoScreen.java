package com.example.HardwareInfo;

import java.util.HashMap;
import java.util.Map;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import java.util.concurrent.Executor;

public final class HardwareInfoScreen extends Screen {

    private static final String TAG = "HardwareInfoScreen";
    private static final String ACTION_SEND_CAR_DATA = "com.example.HardwareInfo.SEND_CAR_DATA";

    private boolean mHasModelPermission;
    private boolean mHasEnergyProfilePermission;
    private final Executor mCarHardwareExecutor;

    private Model mModel;
    private EnergyProfile mEnergyProfile;

    private final OnCarDataAvailableListener<Model> mModelListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received model information: " + data);
            mModel = data;
            broadcastCarData();
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<EnergyProfile> mEnergyProfileListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received energy profile information: " + data);
            mEnergyProfile = data;
            broadcastCarData();
            invalidate();
        }
    };

    public HardwareInfoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mCarHardwareExecutor = ContextCompat.getMainExecutor(getCarContext());
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                CarHardwareManager carHardwareManager = getCarContext().getCarService(CarHardwareManager.class);
                CarInfo carInfo = carHardwareManager.getCarInfo();
                fetchCarInfo(carInfo);
            }
        });
    }

    private void fetchCarInfo(CarInfo carInfo) {
        mModel = null;
        try {
            carInfo.fetchModel(mCarHardwareExecutor, mModelListener);
            mHasModelPermission = true;
        } catch (SecurityException e) {
            mHasModelPermission = false;
        }

        mEnergyProfile = null;
        try {
            carInfo.fetchEnergyProfile(mCarHardwareExecutor, mEnergyProfileListener);
            mHasEnergyProfilePermission = true;
        } catch (SecurityException e) {
            mHasEnergyProfilePermission = false;
        }
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();
        if (allInfoAvailable()) {
            addModelRow(paneBuilder);
            addEnergyProfileRow(paneBuilder);
        } else {
            paneBuilder.setLoading(true);
        }
        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.BACK)
                .setTitle(getCarContext().getString(R.string.car_hardware_info))
                .build();
    }

    private void addModelRow(Pane.Builder paneBuilder) {
        Row.Builder modelRowBuilder = new Row.Builder()
                .setTitle(getCarContext().getString(R.string.model_info));
        if (!mHasModelPermission) {
            modelRowBuilder.addText(getCarContext().getString(R.string.no_model_permission));
        } else {
            modelRowBuilder.addText(getModelInfo());
        }
        paneBuilder.addRow(modelRowBuilder.build());
    }

    private void addEnergyProfileRow(Pane.Builder paneBuilder) {
        Row.Builder energyProfileRowBuilder = new Row.Builder()
                .setTitle(getCarContext().getString(R.string.energy_profile));
        if (!mHasEnergyProfilePermission) {
            energyProfileRowBuilder.addText(getCarContext().getString(R.string.no_energy_profile_permission));
        } else {
            energyProfileRowBuilder.addText(getFuelInfo());
            energyProfileRowBuilder.addText(getEvInfo());
        }
        paneBuilder.addRow(energyProfileRowBuilder.build());
    }

    private String getModelInfo() {
        StringBuilder info = new StringBuilder();
        assert mModel != null;
        appendCarValue(info, mModel.getManufacturer(), R.string.manufacturer_unavailable);
        appendCarValue(info, mModel.getName(), R.string.model_unavailable);
        appendCarYearValue(info, mModel.getYear(), R.string.year_unavailable);
        return info.toString();
    }

    private String getFuelInfo() {
        StringBuilder fuelInfo = new StringBuilder(getCarContext().getString(R.string.fuel_types)).append(": ");
        if (mEnergyProfile == null) {
            throw new AssertionError();
        }
        if (mEnergyProfile.getFuelTypes().getStatus() != CarValue.STATUS_SUCCESS) {
            fuelInfo.append(getCarContext().getString(R.string.unavailable));
        } else {
            assert mEnergyProfile.getFuelTypes().getValue() != null;
            for (int fuelType : mEnergyProfile.getFuelTypes().getValue()) {
                fuelInfo.append(HardwareInfoScreen.FuelTypeUtil.fuelTypeAsString(fuelType)).append(" ");
            }
        }
        return fuelInfo.toString();
    }

    private String getEvInfo() {
        StringBuilder evInfo = new StringBuilder(getCarContext().getString(R.string.ev_connector_types)).append(": ");
        assert mEnergyProfile != null;
        if (mEnergyProfile.getEvConnectorTypes().getStatus() != CarValue.STATUS_SUCCESS) {
            evInfo.append(getCarContext().getString(R.string.unavailable));
        } else {
            assert mEnergyProfile.getEvConnectorTypes().getValue() != null;
            for (int connectorType : mEnergyProfile.getEvConnectorTypes().getValue()) {
                evInfo.append(HardwareInfoScreen.ConnectorUtil.evConnectorAsString(connectorType)).append(" ");
            }
        }
        return evInfo.toString();
    }

    private void appendCarValue(StringBuilder builder, CarValue<String> carValue, int unavailableResId) {
        if (carValue.getStatus() != CarValue.STATUS_SUCCESS) {
            builder.append(getCarContext().getString(unavailableResId)).append(", ");
        } else {
            builder.append(carValue.getValue()).append(", ");
        }
    }

    private void appendCarYearValue(StringBuilder builder, CarValue<Integer> carValue, int unavailableResId) {
        if (carValue.getStatus() != CarValue.STATUS_SUCCESS) {
            builder.append(getCarContext().getString(unavailableResId));
        } else {
            builder.append(carValue.getValue());
        }
    }

    private boolean allInfoAvailable() {
        return (!mHasModelPermission || mModel != null) &&
                (!mHasEnergyProfilePermission || mEnergyProfile != null);
    }

    private void broadcastCarData() {
        if (allInfoAvailable()) {
            StringBuilder carDataBuilder = new StringBuilder();
            if (mModel != null) {
                carDataBuilder.append("Model: ").append(getModelInfo()).append("\n");
            }
            if (mEnergyProfile != null) {
                carDataBuilder.append("Energy Profile: ").append(getFuelInfo()).append("\n");
                carDataBuilder.append("EV Info: ").append(getEvInfo()).append("\n");
            }
            String carData = carDataBuilder.toString();
            Intent intent = new Intent(ACTION_SEND_CAR_DATA);
            intent.putExtra("car_data", carData);
            getCarContext().sendBroadcast(intent);
        }
    }

    static class ConnectorUtil {
        private static final Map<Integer, String> evConnectorTypeMap = new HashMap<>();
        static {
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_J1772, "J1772");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_MENNEKES, "MENNEKES");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_CHADEMO, "CHADEMO");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_COMBO_1, "COMBO_1");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_COMBO_2, "COMBO_2");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_ROADSTER, "TESLA_ROADSTER");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_HPWC, "TESLA_HPWC");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_SUPERCHARGER, "TESLA_SUPERCHARGER");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_GBT, "GBT");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_GBT_DC, "GBT_DC");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_SCAME, "SCAME");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_OTHER, "OTHER");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_UNKNOWN, "UNKNOWN");
        }
        static String evConnectorAsString(int evConnectorType) {
            return evConnectorTypeMap.getOrDefault(evConnectorType, "UNKNOWN");
        }
    }

    static class FuelTypeUtil {
        private static final Map<Integer, String> fuelTypeMap = new HashMap<>();
        static {
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_UNLEADED, "UNLEADED");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LEADED, "LEADED");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_DIESEL_1, "DIESEL_1");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_DIESEL_2, "DIESEL_2");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_BIODIESEL, "BIODIESEL");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_E85, "E85");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LPG, "LPG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_CNG, "CNG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LNG, "LNG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_ELECTRIC, "ELECTRIC");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_HYDROGEN, "HYDROGEN");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_OTHER, "OTHER");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_UNKNOWN, "UNKNOWN");
        }
        static String fuelTypeAsString(int fuelType) {
            return fuelTypeMap.getOrDefault(fuelType, "UNKNOWN");
        }
    }
}
