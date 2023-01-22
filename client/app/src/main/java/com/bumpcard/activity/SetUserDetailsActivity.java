package com.bumpcard.activity;

import static com.bumpcard.config.ApiConfig.API_INFO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
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

            String firstName = userInfo.getString("first_name");
            if (!firstName.equals("null")) {
                binding.inputSetUserFirstName.setText(firstName);
            }

            String lastName = userInfo.getString("last_name");
            if (!lastName.equals("null")) {
                binding.inputSetUserLastName.setText(lastName);
            }

            String headline = userInfo.getString("headline");
            if (!headline.equals("null")) {
                binding.inputSetUserHeadline.setText(headline);
            }

            String email = userInfo.getString("email");
            if (!email.equals("null")) {
                binding.inputSetUserEmail.setText(email);
            }

            String phone = userInfo.getString("phone");
            if (!phone.equals("null")) {
                binding.inputSetUserPhone.setText(phone);
            }
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
        Editable firstName = binding.inputSetUserFirstName.getText();
        Editable lastName = binding.inputSetUserLastName.getText();
        Editable headline = binding.inputSetUserHeadline.getText();
        Editable email = binding.inputSetUserEmail.getText();
        Editable phone = binding.inputSetUserPhone.getText();

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("first_name", firstName != null ? firstName.toString() : "")
                .addFormDataPart("last_name", lastName != null ? lastName.toString() : "")
                .addFormDataPart("headline", headline != null ? headline.toString() : "")
                .addFormDataPart("email", email != null ? email.toString() : "")
                .addFormDataPart("phone", phone != null ? phone.toString() : "")
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
