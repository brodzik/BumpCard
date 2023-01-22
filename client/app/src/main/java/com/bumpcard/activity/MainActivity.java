package com.bumpcard.activity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumpcard.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    ConnectivityManager connectivityManager;
    LocationManager locationManager;
    SensorManager sensorManager;
    AtomicReference<Location> currentLocation = new AtomicReference<>();

    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            Toast.makeText(this, "BumpCard requires permissions to access the internet and location services.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(this, "BumpCard requires internet access.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

//        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(this, "BumpCard requires GPS access.", Toast.LENGTH_LONG).show();
//            finishAndRemoveTask();
//        }

//        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, location -> {
//            currentLocation.set(location);
////            Log.i("BumpCard", "LOCATION " + currentLocation.get().toString());
//        });

//        sensorManager.registerListener(new SensorEventListener() {
//            @Override
//            public void onSensorChanged(SensorEvent event) {
//                float threshold = 2.0f;
//                float x = event.values[0];
//                float y = event.values[1];
//                float z = event.values[2];
//                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
//
//                if (magnitude > threshold) {
//                    Log.i("BumpCard", "BUMP " + magnitude + " " + Arrays.toString(event.values));
//                }
//            }
//
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int i) {
//            }
//        }, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

        binding.goToRegisterButton.setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), RegistrationActivity.class))
        );
        binding.goToSetUserDetailsButton.setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), SetUserDetailsActivity.class))
        );
        binding.goToExchangeCards.setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), ExchangeBusinessCards.class))
        );
        binding.goToViewCards.setOnClickListener(view ->
                startActivity(new Intent(getApplicationContext(), ViewBusinessCards.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isUserLoggedIn()) {
            binding.goToRegisterButton.setVisibility(View.GONE);
            binding.goToSetUserDetailsButton.setVisibility(View.VISIBLE);
            binding.goToExchangeCards.setVisibility(View.VISIBLE);
            binding.goToViewCards.setVisibility(View.VISIBLE);
        } else {
            binding.goToRegisterButton.setVisibility(View.VISIBLE);
            binding.goToSetUserDetailsButton.setVisibility(View.GONE);
            binding.goToExchangeCards.setVisibility(View.GONE);
            binding.goToViewCards.setVisibility(View.GONE);
        }
    }

    private boolean isUserLoggedIn() {
        String api_key = sharedPreferences.getString("api_key", "");
        return !api_key.isEmpty();
    }
}
