package com.bumpcard;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrationActivity extends AppCompatActivity {
    private static final String API_URL = "http://192.168.1.100:5000/register";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        MaterialButton registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(view -> {
            Request request = buildRequest();
            int status = registerNewUser(request);
            Toast.makeText(getApplicationContext(), "" + status, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private Request buildRequest() {
        TextInputEditText inputUsername = findViewById(R.id.input_register_username);
        TextInputEditText inputPassword = findViewById(R.id.input_register_password);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", inputUsername.getText().toString())
                .addFormDataPart("password", inputPassword.getText().toString())
                .build();
        return new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();
    }

    private int registerNewUser(Request request) {
        try {
            return EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    Log.i("BumpCard", "SERVER RESPONSE " + response.body().string());
                    return response.code();
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
