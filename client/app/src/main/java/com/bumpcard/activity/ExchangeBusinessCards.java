package com.bumpcard.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumpcard.R;
import com.bumpcard.databinding.ActivityExchangeBusinessCardsBinding;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExchangeBusinessCards extends AppCompatActivity {
    private static final String API_URL = "http://127.0.0.1:5000/bump";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private ActivityExchangeBusinessCardsBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExchangeBusinessCardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
    }

    public void prepareBump(View view) {
        binding.step1Card.setCardBackgroundColor(getResources().getColor(R.color.step_done_background));
        binding.step1Header.setTextColor(getResources().getColor(R.color.step_done_font));
        binding.readyButton.setVisibility(View.GONE);
        binding.simulateBump.setVisibility(View.VISIBLE);

        // TODO: Tutaj należy uruchomić wykrywanie zderzeń
    }

    public void registerBump(View view) {
        // TODO: Ta metoda powinna być wywoływana przy wykrywaniu zderzeń zamiast w odpowiedzi na przycisk (zbędny parametr View)
        binding.step2Card.setCardBackgroundColor(getResources().getColor(R.color.step_done_background));
        binding.step2Header.setTextColor(getResources().getColor(R.color.step_done_font));
        binding.simulateBump.setVisibility(View.GONE);
        binding.confirmQuestion.setVisibility(View.VISIBLE);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("timestamp", "1672876737207")
                .addFormDataPart("latitude", "52.2297")
                .addFormDataPart("longitude", "21.017532")
                .addFormDataPart("magnitude", "1.5")
                .build();
        Request request = new Request.Builder()
                .url(API_URL)
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
