package com.bumpcard.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumpcard.databinding.ActivitySetUserDetailsBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SetUserDetailsActivity extends AppCompatActivity {
    private static final String API_URL = "http://192.168.1.100:5000/info";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private ActivitySetUserDetailsBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetUserDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        binding.setUserDetailsButton.setOnClickListener(view -> {
            String response = setUserDetails();
            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private String setUserDetails() {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("first_name", binding.inputSetUserFirstName.getText().toString())
                .addFormDataPart("last_name", binding.inputSetUserLastName.getText().toString())
                .addFormDataPart("headline", binding.inputSetUserHeadline.getText().toString())
                .addFormDataPart("email", binding.inputSetUserEmail.getText().toString())
                .addFormDataPart("phone", binding.inputSetUserPhone.getText().toString())
                .build();
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();
        try {
            return EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    int code = response.code();
                    Log.d("SetUserDetailsActivity", "Response code: " + code);

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        Log.d("SetUserDetailsActivity", "Response body is null");
                        return "Error";
                    }
                    String responseText = responseBody.string();
                    Log.d("SetUserDetailsActivity", "Response body: " + responseText);
                    JSONObject responseJson = new JSONObject(responseText);
                    return responseJson.getString("msg");
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return "Unknown error: " + e.getMessage();
        }
    }
}
