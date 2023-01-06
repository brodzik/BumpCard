package com.bumpcard;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    ConnectivityManager connectivityManager;
    LocationManager locationManager;
    SensorManager sensorManager;
    AtomicReference<Location> currentLocation = new AtomicReference<>();

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            Toast.makeText(this, "BumpCard requires permissions to access the internet and location services.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(this, "BumpCard requires internet access.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "BumpCard requires GPS access.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, location -> {
            currentLocation.set(location);
            Log.i("BumpCard", "LOCATION " + currentLocation.get().toString());
        });

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float threshold = 2.0f;
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float magnitude = (float) Math.sqrt(x*x + y*y + z*z);

                if (magnitude > threshold) {
                    Log.i("BumpCard", "BUMP " + magnitude + " " + Arrays.toString(event.values));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}
        }, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("username", "user1")
                .addFormDataPart("password", "password1")
                .build();
        Request request = new Request.Builder()
                .url("http://192.168.1.100:5000/register")
                .method("POST", body)
                .build();

        executorService.submit(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.i("BumpCard", "HTTP status: " + response.code());
                Log.i("BumpCard", "SERVER RESPONSE " + response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
