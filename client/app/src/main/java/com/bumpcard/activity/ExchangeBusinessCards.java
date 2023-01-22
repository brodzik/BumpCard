package com.bumpcard.activity;

import static com.bumpcard.config.ApiConfig.API_BUMP;
import static java.lang.Math.abs;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumpcard.R;
import com.bumpcard.databinding.ActivityExchangeBusinessCardsBinding;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExchangeBusinessCards extends AppCompatActivity {
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private ActivityExchangeBusinessCardsBinding binding;
    private SharedPreferences sharedPreferences;

    SensorManager sensorManager;
    LocationManager locationManager;
    AtomicReference<Location> currentLocation = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExchangeBusinessCardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "BumpCard requires GPS access.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "BumpCard requires permissions to access the internet and location services.", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0, location -> {
            currentLocation.set(location);
        });
    }

    public void prepareBump(View view) {
        binding.step1Card.setCardBackgroundColor(getResources().getColor(R.color.step_done_background));
        binding.step1Header.setTextColor(getResources().getColor(R.color.step_done_font));
        binding.readyButton.setVisibility(View.GONE);
        binding.simulateBump.setVisibility(View.VISIBLE);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float threshold = 3.0f;
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

                if (abs(x) > threshold) {
                    if(currentLocation.get() == null) {
                        Log.i("Location", "Null last location exception");

                    }
                    else {
                        sensorManager.unregisterListener(this);
                        long time = System.currentTimeMillis();
                        Log.i("BumpCard", "BUMP " + magnitude + " " + Arrays.toString(event.values));
                        Log.i("Time", "Time of bump " + time);
                        Log.i("BumpCard", "LOCATION " + currentLocation.get().toString());
                        registerBump(abs(x), time, currentLocation.get());
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        }, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        // TODO: Tutaj należy uruchomić wykrywanie zderzeń
    }

    public void registerBump(/*View view,*/ double acceleration, long time, Location location) {
        // TODO: Ta metoda powinna być wywoływana przy wykrywaniu zderzeń zamiast w odpowiedzi na przycisk (zbędny parametr View)
        binding.step2Card.setCardBackgroundColor(getResources().getColor(R.color.step_done_background));
        binding.step2Header.setTextColor(getResources().getColor(R.color.step_done_font));
        binding.simulateBump.setVisibility(View.GONE);
        binding.confirmQuestion.setVisibility(View.VISIBLE);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("timestamp", String.valueOf(time))
                .addFormDataPart("latitude", String.valueOf(location.getLatitude()))
                .addFormDataPart("longitude", String.valueOf(location.getLongitude()))
                .addFormDataPart("magnitude", String.valueOf(acceleration))
                .build();
        Request request = new Request.Builder()
                .url(API_BUMP)
                .post(body)
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();
        try {
            EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    int code = response.code();
                    Log.i("ExchangeBusinessCards", "response code: " + code);
                } catch (IOException e) {
                    Log.e("ExchangeBusinessCards", e.getMessage());
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("ExchangeBusinessCards", e.getMessage());
        }
    }

    public void declineExchange(View view) {
        finish();
    }

    public void confirmExchange(View view) {
        finish();
    }
}