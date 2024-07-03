package com.example.HardwareInfo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.CarHardwareManager;
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.OnCarDataAvailableListener;
import androidx.car.app.hardware.info.Accelerometer;
import androidx.car.app.hardware.info.CarHardwareLocation;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;
import androidx.car.app.hardware.info.Compass;
import androidx.car.app.hardware.info.EnergyLevel;
import androidx.car.app.hardware.info.EnergyProfile;
import androidx.car.app.hardware.info.Gyroscope;
import androidx.car.app.hardware.info.Mileage;
import androidx.car.app.hardware.info.Model;
import androidx.car.app.hardware.info.Speed;
import androidx.car.app.hardware.info.TollCard;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@ExperimentalCarApi
public final class HardwareInfoScreen extends Screen {
    private static final String TAG = "HardwareInfoScreen";
    private static final String ACTION_SEND_CAR_DATA = "com.example.HardwareInfo.SEND_CAR_DATA";
    private static final String ACTION_REQUEST_CAR_DATA = "com.example.HardwareInfo.REQUEST_CAR_DATA";

    private final Executor mCarHardwareExecutor;

    private Model mModel;
    private EnergyProfile mEnergyProfile;
    private Speed mSpeed;
    private EnergyLevel mEnergyLevel;
    private TollCard mTollCard;
    private Mileage mMileage;
    private Accelerometer mAccelerometer;
    private Gyroscope mGyroscope;
    private Compass mCompass;
    private CarHardwareLocation mCarHardwareLocation;

    private Runnable mRequestRenderRunnable;

    private boolean mHasSpeedPermission;
    private boolean mHasEnergyLevelPermission;
    private boolean mHasTollCardPermission;
    private boolean mHasMileagePermission;
    private boolean mHasAccelerometerPermission;
    private boolean mHasGyroscopePermission;
    private boolean mHasCompassPermission;
    private boolean mHasCarHardwareLocationPermission;

    private boolean sensorsEnabled = true;

    Intent sendCarDataIntent = new Intent(ACTION_SEND_CAR_DATA);

    private final OnCarDataAvailableListener<Model> mModelListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received model information: " + data);
            mModel = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<EnergyProfile> mEnergyProfileListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received energy profile information: " + data);
            mEnergyProfile = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<Speed> mSpeedListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received speed information: " + data);
            mSpeed = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<EnergyLevel> mEnergyLevelListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received energy level information: " + data);
            mEnergyLevel = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<TollCard> mTollListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received toll information:" + data);
            mTollCard = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<Mileage> mMileageListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received mileage: " + data);
            mMileage = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<Accelerometer> mAccelerometerListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received accelerometer: " + data);
            mAccelerometer = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<Gyroscope> mGyroscopeListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received gyroscope: " + data);
            mGyroscope = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<Compass> mCompassListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received compass: " + data);
            mCompass = data;
            invalidate();
        }
    };

    private final OnCarDataAvailableListener<CarHardwareLocation> mCarLocationListener = data -> {
        synchronized (this) {
            Log.i(TAG, "Received car location: " + data);
            mCarHardwareLocation = data;
            invalidate();
        }
    };

    private final BroadcastReceiver requestCarDataReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, @NonNull Intent intent) {
            if (ACTION_REQUEST_CAR_DATA.equals(intent.getAction())) {
                Log.d(TAG, "Car data request received");
                broadcastCarData();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public HardwareInfoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mCarHardwareExecutor = ContextCompat.getMainExecutor(getCarContext());
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onCreate(@NonNull LifecycleOwner owner) {
                fetchCarInfo(mRequestRenderRunnable);
                getCarContext().registerReceiver(requestCarDataReceiver, new IntentFilter(ACTION_REQUEST_CAR_DATA));
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                getCarContext().unregisterReceiver(requestCarDataReceiver);
            }
        });
    }

    private void fetchCarInfo(@NonNull Runnable onChangeListener) {
        mRequestRenderRunnable = onChangeListener;
        CarHardwareManager carHardwareManager = getCarContext().getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        CarSensors carSensors = carHardwareManager.getCarSensors();

        mModel = null;
        carInfo.fetchModel(mCarHardwareExecutor, mModelListener);

        mEnergyProfile = null;
        carInfo.fetchEnergyProfile(mCarHardwareExecutor, mEnergyProfileListener);

        mSpeed = null;
        try {
            carInfo.addSpeedListener(mCarHardwareExecutor, mSpeedListener);
            mHasSpeedPermission = true;
        } catch (SecurityException e) {
            mHasSpeedPermission = false;
        }

        mEnergyLevel = null;
        try {
            carInfo.addEnergyLevelListener(mCarHardwareExecutor, mEnergyLevelListener);
            mHasEnergyLevelPermission = true;
        } catch (SecurityException e) {
            mHasEnergyLevelPermission = false;
        }

        mTollCard = null;
        try {
            carInfo.addTollListener(mCarHardwareExecutor, mTollListener);
            mHasTollCardPermission = true;
        } catch (SecurityException e) {
            mHasTollCardPermission = false;
        }

        mMileage = null;
        try {
            carInfo.addMileageListener(mCarHardwareExecutor, mMileageListener);
            mHasMileagePermission = true;
        } catch (SecurityException e) {
            mHasMileagePermission = false;
        }

        mCompass = null;
        try {
            carSensors.addCompassListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor, mCompassListener);
            mHasCompassPermission = true;
        } catch (SecurityException e) {
            mHasCompassPermission = false;
        }

        mGyroscope = null;
        try {
            carSensors.addGyroscopeListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor, mGyroscopeListener);
            mHasGyroscopePermission = true;
        } catch (SecurityException e) {
            mHasGyroscopePermission = false;
        }

        mAccelerometer = null;
        try {
            carSensors.addAccelerometerListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor, mAccelerometerListener);
            mHasAccelerometerPermission = true;
        } catch (SecurityException e) {
            mHasAccelerometerPermission = false;
        }

        mCarHardwareLocation = null;
        try {
            carSensors.addCarHardwareLocationListener(CarSensors.UPDATE_RATE_NORMAL, mCarHardwareExecutor, mCarLocationListener);
            mHasCarHardwareLocationPermission = true;
        } catch (SecurityException e) {
            mHasCarHardwareLocationPermission = false;
        }
    }

    private void removeCarInfoListeners() {
        CarHardwareManager carHardwareManager = getCarContext().getCarService(CarHardwareManager.class);
        CarInfo carInfo = carHardwareManager.getCarInfo();
        CarSensors carSensors = carHardwareManager.getCarSensors();

        if (mHasSpeedPermission) {
            carInfo.removeSpeedListener(mSpeedListener);
        }

        if (mHasEnergyLevelPermission) {
            carInfo.removeEnergyLevelListener(mEnergyLevelListener);
        }

        if (mHasTollCardPermission) {
            carInfo.removeTollListener(mTollListener);
        }

        if (mHasMileagePermission) {
            carInfo.removeMileageListener(mMileageListener);
        }

        if (mHasCompassPermission) {
            carSensors.removeCompassListener(mCompassListener);
        }

        if (mHasGyroscopePermission) {
            carSensors.removeGyroscopeListener(mGyroscopeListener);
        }

        if (mHasAccelerometerPermission) {
            carSensors.removeAccelerometerListener(mAccelerometerListener);
        }

        if (mHasCarHardwareLocationPermission) {
            carSensors.removeCarHardwareLocationListener(mCarLocationListener);
        }
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();

        if (allInfoAvailable()) {
            addModelRow(paneBuilder);
            addEnergyProfileRow(paneBuilder);
            addSpeedRow(paneBuilder);
            addEnergyLevelRow(paneBuilder);
            addTollCardRow(paneBuilder);
            addMileageRow(paneBuilder);
            addAccelerometerRow(paneBuilder);
            addGyroscopeRow(paneBuilder);
            addCompassRow(paneBuilder);
            addCarLocationRow(paneBuilder);
        } else {
            paneBuilder.addRow(new Row.Builder().setTitle("Gathering car information...").build());
        }

        paneBuilder.addAction(new Action.Builder()
                .setTitle(getCarContext().getString(R.string.request_permissions_title))
                .setOnClickListener(() -> getScreenManager().push(new RequestPermissionScreen(getCarContext())))
                .build());

        paneBuilder.addAction(new Action.Builder()
                .setTitle(sensorsEnabled ? getCarContext().getString(R.string.disable_sensors_title) : getCarContext().getString(R.string.fetch_sensors_title))
                .setOnClickListener(() -> {
                    if (sensorsEnabled) {
                        disableSensors();
                    } else {
                        fetchSensors();
                    }
                    sensorsEnabled = !sensorsEnabled;
                    invalidate();
                })
                .build());

        return new PaneTemplate.Builder(paneBuilder.build())
                .setTitle("Car Information")
                .setHeaderAction(Action.BACK)
                .build();
    }

    private void fetchSensors() {
        fetchCarInfo(mRequestRenderRunnable);
    }

    private void disableSensors() {
        removeCarInfoListeners();
    }

    //CAR UI (SHOULD BE REMOVED)
    private void addModelRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder modelRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.model_info));
        modelRowBuilder.addText(getModelInfo());
        paneBuilder.addRow(modelRowBuilder.build());
    }

    private void addEnergyProfileRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder energyProfileRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.energy_profile));
        energyProfileRowBuilder.addText(getFuelInfo());
        energyProfileRowBuilder.addText(getEvInfo());
        paneBuilder.addRow(energyProfileRowBuilder.build());
    }

    private void addSpeedRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder speedRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.speed));
        speedRowBuilder.addText(getSpeedInfo());
        paneBuilder.addRow(speedRowBuilder.build());
    }

    private void addEnergyLevelRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder energyLevelRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.energy_level));
        energyLevelRowBuilder.addText(getFuelPercent());
        energyLevelRowBuilder.addText(getEnergyIsLow());
        paneBuilder.addRow(energyLevelRowBuilder.build());
    }

    private void addTollCardRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder tollCardRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.toll_card));
        tollCardRowBuilder.addText(getTollCardInfo());
        paneBuilder.addRow(tollCardRowBuilder.build());
    }

    private void addMileageRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder mileageRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.mileage));
        mileageRowBuilder.addText(getMileageInfo());
        paneBuilder.addRow(mileageRowBuilder.build());
    }

    private void addAccelerometerRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder accelerometerRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.accelerometer));
        accelerometerRowBuilder.addText(getAccelerometerInfo());
        paneBuilder.addRow(accelerometerRowBuilder.build());
    }

    private void addGyroscopeRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder gyroscopeRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.gyroscope));
        gyroscopeRowBuilder.addText(getGyroscopeInfo());
        paneBuilder.addRow(gyroscopeRowBuilder.build());
    }

    private void addCompassRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder compassRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.compass));
        compassRowBuilder.addText(getCompassInfo());
        paneBuilder.addRow(compassRowBuilder.build());
    }

    private void addCarLocationRow(@NonNull Pane.Builder paneBuilder) {
        Row.Builder carLocationRowBuilder = new Row.Builder().setTitle(getCarContext().getString(R.string.car_location));
        carLocationRowBuilder.addText(getCarLocationInfo());
        paneBuilder.addRow(carLocationRowBuilder.build());
    }

    @NonNull
    private String getModelInfo() {
        StringBuilder info = new StringBuilder();
        appendCarStringValue(info, mModel.getManufacturer(), R.string.manufacturer_unavailable);
        appendCarStringValue(info, mModel.getName(), R.string.model_unavailable);
        appendCarIntValue(info, mModel.getYear(), R.string.year_unavailable);
        return info.toString();
    }

    @NonNull
    private String getFuelInfo() {
        StringBuilder fuelInfo = new StringBuilder(getCarContext().getString(R.string.fuel_types)).append(": ");
        assert mEnergyProfile.getFuelTypes().getValue() != null;
        for (int fuelType : mEnergyProfile.getFuelTypes().getValue()) {
            fuelInfo.append(FuelTypeUtil.fuelTypeAsString(fuelType)).append(" ");
        }
        return fuelInfo.toString();
    }

    @NonNull
    private String getEvInfo() {
        StringBuilder evInfo = new StringBuilder(getCarContext().getString(R.string.ev_connector_types)).append(": ");
        assert mEnergyProfile.getEvConnectorTypes().getValue() != null;
        for (int connectorType : mEnergyProfile.getEvConnectorTypes().getValue()) {
            evInfo.append(HardwareInfoScreen.ConnectorUtil.evConnectorAsString(connectorType)).append(" ");
        }
        return evInfo.toString();
    }

    @NonNull
    private String getSpeedInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.speed)).append(": ");
        if (mSpeed != null) {
            appendCarFloatValue(info, mSpeed.getDisplaySpeedMetersPerSecond(), R.string.speed_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.speed_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getFuelPercent() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.fuel_level)).append(": ");
        if (mEnergyLevel != null) {
            appendCarFloatValue(info, mEnergyLevel.getFuelPercent(), R.string.fuel_level_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.fuel_level_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getEnergyIsLow() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.fuel_low)).append(": ");
        if (mEnergyLevel != null) {
            appendCarBoolValue(info, mEnergyLevel.getEnergyIsLow(), R.string.fuel_level_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.fuel_level_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getTollCardInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.toll_card_info)).append(": ");
        if (mTollCard != null) {
            appendCarIntValue(info, mTollCard.getCardState(), R.string.toll_card_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.toll_card_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getMileageInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.mileage_info)).append(": ");
        if (mMileage != null) {
            appendCarFloatValue(info, mMileage.getOdometerMeters(), R.string.mileage_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.mileage_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getAccelerometerInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.accelerometer_info)).append("\n[X] [Y] [Z]: ");
        if (mAccelerometer != null) {
            appendCarListValue(info, mAccelerometer.getForces(), R.string.accelerometer_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.accelerometer_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getGyroscopeInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.gyroscope_info)).append("\n[X] [Y] [Z]: ");
        if (mGyroscope != null) {
            appendCarListValue(info, mGyroscope.getRotations(), R.string.gyroscope_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.gyroscope_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getCompassInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.compass_info)).append("\n[Bearing] [Pitch] [Roll]: ");
        if (mCompass != null) {
            appendCarListValue(info, mCompass.getOrientations(), R.string.compass_unavailable);
        } else {
            info.append(getCarContext().getString(R.string.compass_unavailable));
        }
        return info.toString();
    }

    @NonNull
    private String getCarLocationInfo() {
        StringBuilder info = new StringBuilder(getCarContext().getString(R.string.car_location_info)).append(": ");
        if (mCarHardwareLocation != null) {
            info.append("\n\tLatitude: ").append(mCarHardwareLocation.getLocation().getValue().getLatitude());
            info.append("\n\tLongitude: ").append(mCarHardwareLocation.getLocation().getValue().getLongitude());
        } else {
            info.append(getCarContext().getString(R.string.car_location_unavailable));
        }
        return info.toString();
    }

    private void appendCarStringValue(@NonNull StringBuilder builder, @NonNull CarValue<String> carValue, int unavailableResId) {
        builder.append(carValue.getStatus() == CarValue.STATUS_SUCCESS ? carValue.getValue() : getCarContext().getString(unavailableResId)).append(", ");
    }

    private void appendCarIntValue(@NonNull StringBuilder builder, @NonNull CarValue<Integer> carValue, int unavailableResId) {
        builder.append(carValue.getStatus() == CarValue.STATUS_SUCCESS ? carValue.getValue() : getCarContext().getString(unavailableResId));
    }

    @SuppressLint("DefaultLocale")
    private void appendCarFloatValue(@NonNull StringBuilder builder, @NonNull CarValue<Float> carValue, int unavailableResId) {
        builder.append(carValue.getStatus() == CarValue.STATUS_SUCCESS ? String.format("%.2f", carValue.getValue()) : getCarContext().getString(unavailableResId));
    }

    private void appendCarBoolValue(@NonNull StringBuilder builder, @NonNull CarValue<Boolean> carValue, int unavailableResId) {
        builder.append(carValue.getStatus() == CarValue.STATUS_SUCCESS ? carValue.getValue() : getCarContext().getString(unavailableResId));
    }

    @SuppressLint("DefaultLocale")
    private void appendCarListValue(@NonNull StringBuilder builder, @NonNull CarValue<List<Float>> carValue, int unavailableResId) {
        if (carValue.getStatus() == CarValue.STATUS_SUCCESS && carValue.getValue() != null) {
            List<Float> values = carValue.getValue();
            for (int i = 0; i < values.size(); i++) {
                builder.append(String.format("%.2f", values.get(i)));
                if (i < values.size() - 1) {
                    builder.append(", ");
                }
            }
        } else {
            builder.append(getCarContext().getString(unavailableResId));
        }
    }

    private boolean allInfoAvailable() {
        return mModel != null
                && mEnergyProfile != null
                && (mSpeed != null || !mHasSpeedPermission)
                && (mEnergyLevel != null || !mHasEnergyLevelPermission)
                && (mTollCard != null || !mHasTollCardPermission)
                && (mMileage != null || !mHasMileagePermission)
                && (mAccelerometer != null || !mHasAccelerometerPermission)
                && (mGyroscope != null || !mHasGyroscopePermission)
                && (mCompass != null || !mHasCompassPermission)
                && (mCarHardwareLocation != null || !mHasCarHardwareLocationPermission)
                ;
    }

    private void broadcastCarData() {
        if (allInfoAvailable()) {
            sendCarDataIntent.putExtra("MODEL_INFO", "\n\t" + getModelInfo());
            sendCarDataIntent.putExtra("ENERGY_PROFILE_INFO", "\n\t" + getFuelInfo() + "\n\t" + getEvInfo());
            sendCarDataIntent.putExtra("SPEED_INFO", "\n\t" + getSpeedInfo());
            sendCarDataIntent.putExtra("ENERGY_LEVEL_INFO", "\n\t" + getFuelPercent() + "\n\t" + getEnergyIsLow());
            sendCarDataIntent.putExtra("TOLL_CARD_INFO", "\n\t" + getTollCardInfo());
            sendCarDataIntent.putExtra("MILEAGE_INFO", "\n\t" + getMileageInfo());
            sendCarDataIntent.putExtra("ACCELEROMETER_INFO", "\n\t" + getAccelerometerInfo());
            sendCarDataIntent.putExtra("GYROSCOPE_INFO", "\n\t" + getGyroscopeInfo());
            sendCarDataIntent.putExtra("COMPASS_INFO", "\n\t" + getCompassInfo());
            sendCarDataIntent.putExtra("CAR_LOCATION_INFO", "\n\t" + getCarLocationInfo());
            getCarContext().sendBroadcast(sendCarDataIntent);
        }
    }

    static class ConnectorUtil {
        private static final Map<Integer, String> evConnectorTypeMap = new HashMap<>();
        static {
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_J1772, "J1772");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_MENNEKES, "Mennekes");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_CHADEMO, "CHAdeMO");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_COMBO_1, "CCS Combo-1");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_COMBO_2, "CCS Combo-2");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_ROADSTER, "Tesla-Roadster");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_HPWC, "Tesla-HPWC");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_TESLA_SUPERCHARGER, "Tesla-Supercharger");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_GBT, "GB/T-AC");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_GBT_DC, "GB/T-DC");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_SCAME, "SCAME");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_OTHER, "Other");
            evConnectorTypeMap.put(EnergyProfile.EVCONNECTOR_TYPE_UNKNOWN, "UNKNOWN");
        }
        static String evConnectorAsString(int evConnectorType) {
            return evConnectorTypeMap.getOrDefault(evConnectorType, "UNKNOWN");
        }
    }

    static class FuelTypeUtil {
        private static final Map<Integer, String> fuelTypeMap = new HashMap<>();
        static {
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_UNLEADED, "Unleaded");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LEADED, "Leaded");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_DIESEL_1, "Diesel-1");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_DIESEL_2, "Diesel-2");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_BIODIESEL, "Biodiesel");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_E85, "E85");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LPG, "LPG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_CNG, "CNG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_LNG, "LNG");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_ELECTRIC, "Electric");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_HYDROGEN, "Hydrogen");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_OTHER, "Other");
            fuelTypeMap.put(EnergyProfile.FUEL_TYPE_UNKNOWN, "Unknown");
        }
        static String fuelTypeAsString(int fuelType) {
            return fuelTypeMap.getOrDefault(fuelType, "Unknown");
        }
    }
}
