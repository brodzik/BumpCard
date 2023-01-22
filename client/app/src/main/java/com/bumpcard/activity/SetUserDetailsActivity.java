package com.bumpcard.activity;

import static com.bumpcard.config.ApiConfig.API_INFO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumpcard.databinding.ActivitySetUserDetailsBinding;

import org.json.JSONException;
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

    @Override
    protected void onResume() {
        super.onResume();

        Request request = new Request.Builder()
                .url(API_INFO)
                .get()
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();
        try {
            JSONObject userInfo = new JSONObject(getUserInfo(request));
            binding.inputSetUserFirstName.setText(userInfo.getString("first_name"));
            binding.inputSetUserLastName.setText(userInfo.getString("last_name"));
            binding.inputSetUserHeadline.setText(userInfo.getString("headline"));
            binding.inputSetUserEmail.setText(userInfo.getString("email"));
            binding.inputSetUserPhone.setText(userInfo.getString("phone"));
        } catch (JSONException e) {
            Log.e("SetUserDetailsActivity", "onResume(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getUserInfo(Request request) {
        try {
            return EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    int code = response.code();
                    Log.d("SetUserDetailsActivity", "GET /info response code: " + code);
                    if (code != 200) {
                        return "";
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        Log.d("SetUserDetailsActivity", "GET /info response body is null");
                        return "";
                    }

                    String responseText = responseBody.string();
                    Log.d("SetUserDetailsActivity", "GET /info response body: " + responseText);
                    return responseText;
                } catch (IOException e) {
                    Log.e("SetUserDetailsActivity", e.getMessage());
                    e.printStackTrace();
                    return "";
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("SetUserDetailsActivity", e.getMessage());
            e.printStackTrace();
            return "";
        }
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
                .url(API_INFO)
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
