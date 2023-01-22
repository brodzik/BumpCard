package com.bumpcard.activity;

import static com.bumpcard.config.ApiConfig.API_CONNECTION;
import static com.bumpcard.config.ApiConfig.API_INFO;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.bumpcard.R;
import com.bumpcard.databinding.ActivityExchangeBusinessCardsBinding;
import com.bumpcard.databinding.ActivityViewBusinessCardsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ViewBusinessCards extends AppCompatActivity {
//    private static final String API_URL = "http://192.168.1.76:5000/connection";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder().build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private ActivityViewBusinessCardsBinding binding;
    private SharedPreferences sharedPreferences;

    private ArrayList<BusinessCard> businessCardList;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewBusinessCardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);

        businessCardList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);

        setCardsInfo();
        setAdapter();
    }

    private void setAdapter() {
        RecyclerAdapter adapter = new RecyclerAdapter(businessCardList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    private void setCardsInfo() {
        Request request = new Request.Builder()
                .url(API_CONNECTION)
                .get()
                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                .build();

        try {
            EXECUTOR_SERVICE.submit(() -> {
                try (Response response = CLIENT.newCall(request).execute()) {
                    int code = response.code();
                    ResponseBody body = response.body();
                    String bodyText = body.string();
                    JSONObject responseJson = new JSONObject(bodyText);
                    JSONArray connections = responseJson.getJSONArray("connection");
                    for(int i=0; i < connections.length(); i++) {
                        JSONObject user = connections.getJSONObject(i);
                        Request request_data = new Request.Builder()
                                .url(API_INFO + "/" + user.getString("user2_id"))
                                .get()
                                .addHeader("api_key", sharedPreferences.getString("api_key", ""))
                                .build();
                        try (Response response_data = CLIENT.newCall(request_data).execute()) {
                            int code_data = response_data.code();
                            ResponseBody body_data = response_data.body();
                            String bodyTextData = body_data.string();
                            JSONObject responseJsonData = new JSONObject(bodyTextData);
                            businessCardList.add(
                                    new BusinessCard(responseJsonData.getString("first_name"),
                                            responseJsonData.getString("last_name"),
                                            responseJsonData.getString("phone"),
                                            responseJsonData.getString("email")));
                        }
                        Log.i("User2_id", user.getString("user2_id"));
                    }
                    Log.i("ExchangeBusinessCards", "response code: " + code);
                } catch (IOException e) {
                    Log.e("ExchangeBusinessCards", e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("ExchangeBusinessCards", e.getMessage());
        }

//        businessCardList.add(new BusinessCard("Jan", "Kowalski", "123456789", "jk@gmail.com"));
//        businessCardList.add(new BusinessCard("Adam", "Kowalski", "123456789", "ak@gmail.com"));
//        businessCardList.add(new BusinessCard("Cezary", "Kowalski", "123456789", "ck@gmail.com"));
//        businessCardList.add(new BusinessCard("Bartosz", "Kowalski", "123456789", "bk@gmail.com"));
    }
}