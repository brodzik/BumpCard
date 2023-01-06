package com.bumpcard.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumpcard.R;
import com.bumpcard.databinding.ActivityRegistrationBinding;
import com.google.android.material.textfield.TextInputEditText;

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

public class RegistrationActivity extends AppCompatActivity {
    private static final String API_URL = "http://192.168.1.100:5000/register";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private ActivityRegistrationBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        binding.registerButton.setOnClickListener(view -> {
            Request request = buildRequest();
            int status = registerNewUser(request);
            Toast.makeText(getApplicationContext(), "" + status, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private Request buildRequest() {

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", binding.inputRegisterUsername.getText().toString())
                .addFormDataPart("password", binding.inputRegisterPassword.getText().toString())
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
                    int code = response.code();
                    ResponseBody body = response.body();

                    if (code != 200) {
                        Log.i("RegistrationActivity", "SERVER RESPONSE " + (body != null ? body.string() : "Empty"));
                        return code;
                    }

                    if (body == null) {
                        return -1;
                    }
                    String bodyText = body.string();
                    JSONObject responseJson = new JSONObject(bodyText);
                    String apiKey = responseJson.getString("api_key");
                    Log.i("RegistrationActivity", "api_key: " + apiKey);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("api_key", apiKey);
                    editor.apply();

                    return code;
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
