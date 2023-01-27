package com.bumpcard.activity;

import static com.bumpcard.config.ApiConfig.API_BUMP;
import static com.bumpcard.config.ApiConfig.API_CONNECTION;
import static com.bumpcard.config.ApiConfig.API_CONNECTION_ALLOW;
import static com.bumpcard.config.ApiConfig.API_INFO_BASIC;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import okhttp3.ResponseBody;

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

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float threshold = 3.0f;
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

                if (abs(x) > threshold) {
                    Location currentLocation = ExchangeBusinessCards.this.currentLocation.get();
                    if (currentLocation == null) {
                        Log.i("Location", "Null last location exception");
                    } else {
                        sensorManager.unregisterListener(this);
                        long time = System.currentTimeMillis();
                        Log.i("BumpCard", "BUMP " + magnitude + " " + Arrays.toString(event.values));
                        Log.i("Time", "Time of bump " + time);
                        Log.i("BumpCard", "LOCATION " + currentLocation.toString());
                        registerBump(abs(x), time, currentLocation);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        }, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void registerBump(double acceleration, long time, Location location) {
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

        Request req = new Request.Builder()
                .url(API_CONNECTION)
                .get()
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();

        boolean isConnection = false;
        long waitTime = System.currentTimeMillis();
        while (waitTime + 10_000L > System.currentTimeMillis() && !isConnection) {
            try {
                isConnection = EXECUTOR_SERVICE.submit(() -> {
                    Thread.sleep(500);
                    try (Response response = CLIENT.newCall(req).execute()) {
                        ResponseBody b = response.body();
                        JSONObject responseJson = new JSONObject(b.string());
                        JSONArray connections = responseJson.getJSONArray("connection");
                        if (connections.length() > 0) {
                            for(int i = 0; i < connections.length(); i++){
                                JSONObject con = connections.getJSONObject(i);
                                if(con.getBoolean("active") == false){
                                    return true;
                                }
                            }
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                    return false;
                }).get();
            } catch (Exception e) {
            }
        }
        if (isConnection) {
            Request req2 = new Request.Builder()
                    .url(API_CONNECTION)
                    .get()
                    .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                    .build();

            try {
                String name = EXECUTOR_SERVICE.submit(() -> {
                    try (Response response = CLIENT.newCall(req2).execute()) {
                        ResponseBody b = response.body();
                        JSONObject responseJson = new JSONObject(b.string());
                        JSONArray connections = responseJson.getJSONArray("connection");
                        int index = 0;
                        for(int i = 0; i < connections.length();i++) {
                            JSONObject con = connections.getJSONObject(i);
                            if(con.getBoolean("active") == false){
                                index = i;
                            }
                        }
                        JSONObject user = connections.getJSONObject(index);
                        String connectedUserId = user.getString("user2_id");

                        Request req3 = new Request.Builder()
                                .url(API_INFO_BASIC + connectedUserId)
                                .get()
                                .build();
                        try (Response resp = CLIENT.newCall(req3).execute()) {
                            ResponseBody respBody = resp.body();
                            JSONObject jo = new JSONObject(respBody.string());
                            return jo.getString("first_name") + " " + jo.getString("last_name");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (JSONException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return "";
                }).get();
                binding.exchangeWithUser.setVisibility(View.VISIBLE);
                binding.exchangeWithUser.setText("Confirm exchange with user " + name);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            binding.step2Card.setCardBackgroundColor(getResources().getColor(R.color.step_done_background));
            binding.step2Header.setTextColor(getResources().getColor(R.color.step_done_font));
            binding.confirmQuestion.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getApplicationContext(), "Connection not found", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void declineExchange(View view) {
        finish();
    }

    public void confirmExchange(View view) {
        Request request = new Request.Builder()
                .url(API_CONNECTION)
                .get()
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();

        int responseCode = -1;
        try {
            responseCode = EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    ResponseBody body = response.body();
                    JSONObject responseJson = new JSONObject(body.string());
                    JSONArray connections = responseJson.getJSONArray("connection");
                    JSONObject user = connections.getJSONObject(0);
                    String connectedUserId = user.getString("user2_id");

                    Request allowConnectionRequest = new Request.Builder()
                            .url(API_CONNECTION_ALLOW + connectedUserId)
                            .post(new MultipartBody.Builder().addFormDataPart("as", "df").build())
                            .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                            .build();
                    try (Response allowConnectionResponse = CLIENT.newCall(allowConnectionRequest).execute()) {
                        return allowConnectionResponse.code();
                    }
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (responseCode == 200) {
            Toast.makeText(getApplicationContext(), "Connected with user", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}

class BumpParameters {
    private final long time;
    private final Location location;
    private final float force;

    public BumpParameters(long time, Location location, float force) {
        this.time = time;
        this.location = location;
        this.force = force;
    }

    public long getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }

    public float getForce() {
        return force;
    }
}
